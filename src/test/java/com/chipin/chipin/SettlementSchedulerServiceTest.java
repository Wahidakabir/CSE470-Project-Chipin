package com.chipin.chipin;

import com.chipin.chipin.dto.SchedulerOverview;
import com.chipin.chipin.model.Expense;
import com.chipin.chipin.model.SettlementReminder;
import com.chipin.chipin.model.SettlementSchedule;
import com.chipin.chipin.model.SettlementScheduleRun;
import com.chipin.chipin.repository.*;
import com.chipin.chipin.service.DebtSimplificationService;
import com.chipin.chipin.service.RoundUpService;
import com.chipin.chipin.service.SettlementSchedulerService;
import com.chipin.chipin.support.InMemoryRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Feature 4 service tests. No database: repositories are in-memory stand-ins.
class SettlementSchedulerServiceTest {

    private static final String GROUP = "group_01";
    private static final List<String> MEMBERS = List.of("Afifa", "Mufliha", "Musarrat", "Sayma");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 18); // a Wednesday

    private final ExpenseRepository expenses = InMemoryRepository.create(ExpenseRepository.class);
    private final DebtSettlementRepository settlements = InMemoryRepository.create(DebtSettlementRepository.class);
    private final SettlementReminderRepository reminders = InMemoryRepository.create(SettlementReminderRepository.class);
    private final DebtSimplificationService debts = new DebtSimplificationService(
            expenses, settlements, new RoundUpService(
                    InMemoryRepository.create(RoundUpSettingsRepository.class),
                    InMemoryRepository.create(RoundUpSavingRepository.class)));
    private final SettlementSchedulerService service = new SettlementSchedulerService(
            InMemoryRepository.create(SettlementScheduleRepository.class),
            InMemoryRepository.create(SettlementScheduleRunRepository.class),
            reminders, debts);

    private void owes(String payer, String debtor, double amount) {
        Map<String, Double> splits = new LinkedHashMap<>();
        splits.put(debtor, amount);
        Expense e = new Expense();
        e.setGroupId(GROUP);
        e.setPaidByUserId(payer);
        e.setCategory("Itemized Bill");
        e.setStatus("APPROVED");
        e.setSplits(splits);
        e.setCreatedAt(LocalDateTime.now());
        expenses.save(e);
    }

    private SettlementSchedule monthly(String mode) {
        return service.createSchedule(GROUP, "Mufliha", "MONTHLY", 0, mode, MEMBERS, TODAY);
    }

    private boolean settledUp() {
        return debts.getOverview(GROUP, MEMBERS).isSettledUp();
    }

    @Test
    void newScheduleFirstRunsOnTheNextMatchingDate() {
        SettlementSchedule s = monthly(SettlementSchedule.MODE_REMINDER);

        assertEquals(LocalDate.of(2026, 11, 30), s.getNextRunDate());
        assertTrue(s.isActive());
    }

    @Test
    void createRejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () ->
                service.createSchedule(GROUP, "Mufliha", "MONTHLY", 0, "SOMETHING", MEMBERS, TODAY));
        assertThrows(IllegalArgumentException.class, () ->
                service.createSchedule(GROUP, "Mufliha", "MONTHLY", 0, SettlementSchedule.MODE_AUTO, List.of("Afifa"), TODAY));
        assertThrows(IllegalArgumentException.class, () ->
                service.createSchedule(GROUP, "Mufliha", "WEEKLY", 9, SettlementSchedule.MODE_AUTO, MEMBERS, TODAY));
        assertThrows(IllegalArgumentException.class, () ->
                service.createSchedule(GROUP, " ", "WEEKLY", 5, SettlementSchedule.MODE_AUTO, MEMBERS, TODAY));
    }

    @Test
    void nothingRunsBeforeTheDueDate() {
        owes("Mufliha", "Afifa", 100);
        monthly(SettlementSchedule.MODE_AUTO);

        assertTrue(service.runDueSchedules(TODAY).isEmpty());
        assertFalse(settledUp());
    }

    @Test
    void automaticScheduleSettlesEveryoneOnTheDueDate() {
        owes("Mufliha", "Afifa", 100);
        owes("Sayma", "Musarrat", 50);
        SettlementSchedule s = monthly(SettlementSchedule.MODE_AUTO);

        List<SettlementScheduleRun> runs = service.runDueSchedules(LocalDate.of(2026, 11, 30));

        assertEquals(1, runs.size());
        assertEquals(2, runs.get(0).getPaymentsMade());
        assertEquals(0, new java.math.BigDecimal("150.00").compareTo(runs.get(0).getTotalAmount()));
        assertEquals(SettlementScheduleRun.SCHEDULED, runs.get(0).getTriggeredBy());
        assertTrue(settledUp());
        // moves on to the next month's end
        assertEquals(LocalDate.of(2026, 12, 31), service.getOverview(GROUP, MEMBERS, TODAY)
                .getSchedules().get(0).getNextRunDate());
        assertEquals(s.getId(), runs.get(0).getScheduleId());
    }

    @Test
    void reminderScheduleSendsRemindersAndDoesNotPay() {
        owes("Mufliha", "Afifa", 100);

        monthly(SettlementSchedule.MODE_REMINDER);
        List<SettlementScheduleRun> runs = service.runDueSchedules(LocalDate.of(2026, 11, 30));

        assertEquals(1, runs.get(0).getRemindersSent());
        assertEquals(0, runs.get(0).getPaymentsMade());
        assertFalse(settledUp());
        List<SettlementReminder> open = reminders.findByGroupIdAndDone(GROUP, false);
        assertEquals(1, open.size());
        assertEquals("Afifa", open.get(0).getFromUserId());
        assertEquals("Mufliha", open.get(0).getToUserId());
    }

    @Test
    void runningAgainDoesNotDuplicateReminders() {
        owes("Mufliha", "Afifa", 100);
        SettlementSchedule s = monthly(SettlementSchedule.MODE_REMINDER);

        service.runNow(s.getId(), TODAY);
        service.runNow(s.getId(), TODAY);

        assertEquals(1, reminders.findByGroupIdAndDone(GROUP, false).size());
    }

    @Test
    void remindersClearWhenTheDebtIsPaid() {
        owes("Mufliha", "Afifa", 100);
        SettlementSchedule s = monthly(SettlementSchedule.MODE_REMINDER);
        service.runNow(s.getId(), TODAY);
        debts.settleAll(GROUP, MEMBERS, "Afifa");

        service.runNow(s.getId(), TODAY);

        assertTrue(reminders.findByGroupIdAndDone(GROUP, false).isEmpty());
    }

    @Test
    void runNowDoesNotMoveTheNextDate() {
        owes("Mufliha", "Afifa", 100);
        SettlementSchedule s = monthly(SettlementSchedule.MODE_AUTO);

        SettlementScheduleRun run = service.runNow(s.getId(), TODAY);

        assertEquals(SettlementScheduleRun.MANUAL, run.getTriggeredBy());
        assertTrue(settledUp());
        assertEquals(LocalDate.of(2026, 11, 30), service.getOverview(GROUP, MEMBERS, TODAY)
                .getSchedules().get(0).getNextRunDate());
    }

    @Test
    void missedScheduleRunsOnceThenSkipsToTheFuture() {
        owes("Mufliha", "Afifa", 100);
        monthly(SettlementSchedule.MODE_AUTO);

        // app was off for six weeks
        List<SettlementScheduleRun> runs = service.runDueSchedules(LocalDate.of(2027, 1, 12));

        assertEquals(1, runs.size());
        assertTrue(runs.get(0).getNote().contains("It was due on"));
        assertEquals(LocalDate.of(2027, 1, 31), service.getOverview(GROUP, MEMBERS, TODAY)
                .getSchedules().get(0).getNextRunDate());
        assertTrue(service.runDueSchedules(LocalDate.of(2027, 1, 12)).isEmpty());
    }

    @Test
    void pausedSchedulesDoNotRunAndResumeSkipsAheadFromToday() {
        owes("Mufliha", "Afifa", 100);
        SettlementSchedule s = monthly(SettlementSchedule.MODE_AUTO);

        service.setActive(s.getId(), false, TODAY);
        assertTrue(service.runDueSchedules(LocalDate.of(2026, 12, 1)).isEmpty());

        service.setActive(s.getId(), true, LocalDate.of(2026, 12, 1));
        assertEquals(LocalDate.of(2026, 12, 31), service.getOverview(GROUP, MEMBERS, TODAY)
                .getSchedules().get(0).getNextRunDate());
        assertFalse(settledUp());
    }

    @Test
    void settledGroupStillLogsARunWithoutPayments() {
        SettlementSchedule s = monthly(SettlementSchedule.MODE_AUTO);

        SettlementScheduleRun run = service.runNow(s.getId(), TODAY);

        assertEquals(0, run.getPaymentsMade());
        assertTrue(run.getNote().contains("already settled up"));
    }

    @Test
    void deletingASchedulerClosesItsReminders() {
        owes("Mufliha", "Afifa", 100);
        SettlementSchedule s = monthly(SettlementSchedule.MODE_REMINDER);
        service.runNow(s.getId(), TODAY);

        service.deleteSchedule(s.getId());

        assertTrue(reminders.findByGroupIdAndDone(GROUP, false).isEmpty());
        assertTrue(service.getOverview(GROUP, MEMBERS, TODAY).getSchedules().isEmpty());
    }

    @Test
    void reminderCanBeDismissedByHand() {
        owes("Mufliha", "Afifa", 100);
        SettlementSchedule s = monthly(SettlementSchedule.MODE_REMINDER);
        service.runNow(s.getId(), TODAY);
        Long id = reminders.findByGroupIdAndDone(GROUP, false).get(0).getId();

        service.markReminderDone(id);

        assertTrue(reminders.findByGroupIdAndDone(GROUP, false).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.markReminderDone(999L));
    }

    @Test
    void overviewShowsWhatIsNeededRightNow() {
        owes("Mufliha", "Afifa", 100);
        monthly(SettlementSchedule.MODE_REMINDER);

        SchedulerOverview overview = service.getOverview(GROUP, MEMBERS, TODAY);

        assertFalse(overview.isSettledUp());
        assertEquals(1, overview.getPaymentsNeededNow().size());
        assertFalse(overview.hasDueSchedule());
        assertTrue(service.getOverview(GROUP, MEMBERS, LocalDate.of(2026, 11, 30)).hasDueSchedule());
    }
}
