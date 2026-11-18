package com.chipin.chipin.service;

import com.chipin.chipin.dto.DebtEdge;
import com.chipin.chipin.dto.DebtOverview;
import com.chipin.chipin.dto.ScheduleView;
import com.chipin.chipin.dto.SchedulerOverview;
import com.chipin.chipin.model.DebtSettlement;
import com.chipin.chipin.model.SettlementReminder;
import com.chipin.chipin.model.SettlementSchedule;
import com.chipin.chipin.model.SettlementScheduleRun;
import com.chipin.chipin.repository.SettlementReminderRepository;
import com.chipin.chipin.repository.SettlementScheduleRepository;
import com.chipin.chipin.repository.SettlementScheduleRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// Feature 4: settles a group's balances on a schedule, for example at the end of every month.
// A schedule either reminds members to pay or records the payments automatically.
// It reuses the Debt Simplification service (Feature 1), so automatic payments also earn round-up (Feature 3).
// Creating the recurring expenses themselves stays with the Recurring Expenses feature.
@Service
public class SettlementSchedulerService {

    // Shown as "recorded by" on payments the scheduler makes.
    public static final String SCHEDULER_USER = "Settlement scheduler";

    private final SettlementScheduleRepository scheduleRepository;
    private final SettlementScheduleRunRepository runRepository;
    private final SettlementReminderRepository reminderRepository;
    private final DebtSimplificationService debtService;

    public SettlementSchedulerService(SettlementScheduleRepository scheduleRepository,
                                      SettlementScheduleRunRepository runRepository,
                                      SettlementReminderRepository reminderRepository,
                                      DebtSimplificationService debtService) {
        this.scheduleRepository = scheduleRepository;
        this.runRepository = runRepository;
        this.reminderRepository = reminderRepository;
        this.debtService = debtService;
    }

    // ---- reading ----------------------------------------------------------------------------

    public SchedulerOverview getOverview(String groupId, List<String> members, LocalDate today) {
        List<ScheduleView> views = new ArrayList<>();
        for (SettlementSchedule s : scheduleRepository.findByGroupIdOrderByIdDesc(groupId)) {
            views.add(new ScheduleView(s, today));
        }
        List<SettlementScheduleRun> runs = runRepository.findByGroupIdOrderByIdDesc(groupId);
        List<SettlementScheduleRun> recent = runs.size() > 10 ? new ArrayList<>(runs.subList(0, 10)) : runs;
        List<DebtEdge> needed = debtService.getOverview(groupId, members).getSimplifiedPayments();
        return new SchedulerOverview(views, reminderRepository.findByGroupIdAndDone(groupId, false), recent, needed);
    }

    // ---- changing schedules -----------------------------------------------------------------

    // The first run is the next matching date after today, so a new schedule never fires straight away.
    @Transactional
    public SettlementSchedule createSchedule(String groupId, String createdByUserId, String frequency, int dayValue,
                                             String runMode, List<String> members, LocalDate today) {
        if (createdByUserId == null || createdByUserId.isBlank()) {
            throw new IllegalArgumentException("Choose who is setting up the schedule.");
        }
        if (!SettlementSchedule.MODE_REMINDER.equals(runMode) && !SettlementSchedule.MODE_AUTO.equals(runMode)) {
            throw new IllegalArgumentException("Choose reminders or automatic payments.");
        }
        Set<String> distinct = new LinkedHashSet<>();
        if (members != null) {
            for (String m : members) {
                if (m != null && !m.isBlank()) {
                    distinct.add(m.trim());
                }
            }
        }
        if (distinct.size() < 2) {
            throw new IllegalArgumentException("A settlement needs at least two members.");
        }
        SettlementDateCalculator.validate(frequency, dayValue);

        SettlementSchedule schedule = new SettlementSchedule();
        schedule.setGroupId(groupId);
        schedule.setCreatedByUserId(createdByUserId);
        schedule.setFrequency(frequency);
        schedule.setDayValue(dayValue);
        schedule.setRunMode(runMode);
        schedule.setMembersCsv(String.join(",", distinct));
        schedule.setActive(true);
        schedule.setNextRunDate(SettlementDateCalculator.nextAfter(frequency, dayValue, today));
        schedule.setCreatedAt(LocalDateTime.now());
        return scheduleRepository.save(schedule);
    }

    // Pausing keeps the schedule. Resuming picks the next date after today, so nothing runs for the pause.
    @Transactional
    public SettlementSchedule setActive(Long scheduleId, boolean active, LocalDate today) {
        SettlementSchedule schedule = find(scheduleId);
        schedule.setActive(active);
        if (active) {
            schedule.setNextRunDate(SettlementDateCalculator.nextAfter(
                    schedule.getFrequency(), schedule.getDayValue(), today));
        }
        return scheduleRepository.save(schedule);
    }

    @Transactional
    public void deleteSchedule(Long scheduleId) {
        SettlementSchedule schedule = find(scheduleId);
        resolveOpenReminders(schedule);
        scheduleRepository.delete(schedule);
    }

    @Transactional
    public void markReminderDone(Long reminderId) {
        SettlementReminder reminder = reminderRepository.findById(reminderId)
                .orElseThrow(() -> new IllegalArgumentException("That reminder no longer exists."));
        reminder.setDone(true);
        reminderRepository.save(reminder);
    }

    // ---- running ----------------------------------------------------------------------------

    // Runs every active schedule that is due today or was missed while the app was off.
    // A missed schedule runs once, not once per missed date, then moves on to its next date.
    @Transactional
    public List<SettlementScheduleRun> runDueSchedules(LocalDate today) {
        List<SettlementScheduleRun> runs = new ArrayList<>();
        for (SettlementSchedule schedule : scheduleRepository.findByActive(true)) {
            if (SettlementDateCalculator.isDue(schedule.getNextRunDate(), today)) {
                runs.add(execute(schedule, today, SettlementScheduleRun.SCHEDULED, true));
            }
        }
        return runs;
    }

    // "Run now" button: runs the schedule's rule today without moving its next date.
    @Transactional
    public SettlementScheduleRun runNow(Long scheduleId, LocalDate today) {
        return execute(find(scheduleId), today, SettlementScheduleRun.MANUAL, false);
    }

    private SettlementScheduleRun execute(SettlementSchedule schedule, LocalDate today,
                                          String triggeredBy, boolean moveToNextDate) {
        List<String> members = List.of(schedule.getMembersCsv().split(","));
        boolean auto = SettlementSchedule.MODE_AUTO.equals(schedule.getRunMode());

        SettlementScheduleRun run = new SettlementScheduleRun();
        run.setScheduleId(schedule.getId());
        run.setGroupId(schedule.getGroupId());
        run.setRunDate(today);
        run.setRunMode(schedule.getRunMode());
        run.setTriggeredBy(triggeredBy);
        run.setRanAt(LocalDateTime.now());

        try {
            if (auto) {
                payAutomatically(schedule, members, run);
            } else {
                sendReminders(schedule, members, today, run);
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            // One schedule failing must not stop the others. The reason is kept in the history.
            run.setTotalAmount(BigDecimal.ZERO);
            run.setNote("Could not run: " + e.getMessage());
        }

        if (moveToNextDate) {
            if (schedule.getNextRunDate() != null && schedule.getNextRunDate().isBefore(today)) {
                run.setNote(run.getNote() + " It was due on " + schedule.getNextRunDate() + ".");
            }
            schedule.setNextRunDate(SettlementDateCalculator.nextAfter(
                    schedule.getFrequency(), schedule.getDayValue(), today));
        }
        schedule.setLastRunDate(today);
        scheduleRepository.save(schedule);
        return runRepository.save(run);
    }

    private void payAutomatically(SettlementSchedule schedule, List<String> members, SettlementScheduleRun run) {
        List<DebtSettlement> payments = debtService.settleAll(schedule.getGroupId(), members, SCHEDULER_USER);
        BigDecimal total = BigDecimal.ZERO;
        for (DebtSettlement p : payments) {
            total = total.add(p.getAmount());
        }
        resolveOpenReminders(schedule);
        run.setPaymentsMade(payments.size());
        run.setTotalAmount(total);
        run.setNote(payments.isEmpty()
                ? "Everyone was already settled up."
                : "Recorded " + payments.size() + (payments.size() == 1 ? " payment" : " payments")
                    + " totalling ৳" + total + ".");
    }

    // Keeps one open reminder per "who pays whom". A reminder whose debt is gone is closed automatically.
    private void sendReminders(SettlementSchedule schedule, List<String> members, LocalDate today,
                               SettlementScheduleRun run) {
        DebtOverview overview = debtService.getOverview(schedule.getGroupId(), members);
        List<SettlementReminder> open = reminderRepository.findByScheduleIdAndDone(schedule.getId(), false);
        Set<String> stillOwed = new LinkedHashSet<>();
        BigDecimal total = BigDecimal.ZERO;

        for (DebtEdge payment : overview.getSimplifiedPayments()) {
            String key = payment.getFromUserId() + ">" + payment.getToUserId();
            stillOwed.add(key);
            SettlementReminder reminder = null;
            for (SettlementReminder r : open) {
                if ((r.getFromUserId() + ">" + r.getToUserId()).equals(key)) {
                    reminder = r;
                }
            }
            if (reminder == null) {
                reminder = new SettlementReminder();
                reminder.setScheduleId(schedule.getId());
                reminder.setGroupId(schedule.getGroupId());
                reminder.setFromUserId(payment.getFromUserId());
                reminder.setToUserId(payment.getToUserId());
            }
            reminder.setAmount(payment.getAmount());
            reminder.setCreatedOn(today);
            reminder.setDone(false);
            reminderRepository.save(reminder);
            total = total.add(payment.getAmount());
        }
        for (SettlementReminder r : open) {
            if (!stillOwed.contains(r.getFromUserId() + ">" + r.getToUserId())) {
                r.setDone(true);
                reminderRepository.save(r);
            }
        }

        run.setRemindersSent(stillOwed.size());
        run.setTotalAmount(total);
        run.setNote(stillOwed.isEmpty()
                ? "Everyone is settled up, so no reminders were needed."
                : "Sent " + stillOwed.size() + (stillOwed.size() == 1 ? " reminder" : " reminders")
                    + " for ৳" + total + ".");
    }

    private void resolveOpenReminders(SettlementSchedule schedule) {
        for (SettlementReminder r : reminderRepository.findByScheduleIdAndDone(schedule.getId(), false)) {
            r.setDone(true);
            reminderRepository.save(r);
        }
    }

    private SettlementSchedule find(Long scheduleId) {
        Optional<SettlementSchedule> schedule = scheduleRepository.findById(scheduleId);
        return schedule.orElseThrow(() -> new IllegalArgumentException("That schedule no longer exists."));
    }
}
