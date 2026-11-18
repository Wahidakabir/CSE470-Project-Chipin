package com.chipin.chipin.controller.api;

import com.chipin.chipin.dto.SchedulerOverview;
import com.chipin.chipin.model.SettlementSchedule;
import com.chipin.chipin.model.SettlementScheduleRun;
import com.chipin.chipin.service.SettlementSchedulerService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

// JSON API for Feature 4 (Settlement Scheduler). Same logic as the /settlement-scheduler page.
@RestController
@RequestMapping("/api/settlement-scheduler")
public class SettlementSchedulerApiController {

    private static final String DEFAULT_GROUP_ID = "group_01";
    private static final List<String> DEMO_MEMBERS = Arrays.asList("Afifa", "Mufliha", "Musarrat", "Sayma");

    private final SettlementSchedulerService schedulerService;

    public SettlementSchedulerApiController(SettlementSchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    // frequency: WEEKLY or MONTHLY. dayValue: 1-7 for weekly (1 = Monday), 1-31 or 0 (last day) for monthly.
    // runMode: REMINDER or AUTO. members is optional and defaults to the whole group.
    public record CreateRequest(String createdByUserId, String frequency, int dayValue,
                                String runMode, List<String> members) {}

    @GetMapping
    public SchedulerOverview overview() {
        return schedulerService.getOverview(DEFAULT_GROUP_ID, DEMO_MEMBERS, LocalDate.now());
    }

    @PostMapping
    public SettlementSchedule create(@RequestBody CreateRequest request) {
        List<String> members = request.members() == null || request.members().isEmpty()
                ? DEMO_MEMBERS : request.members();
        return schedulerService.createSchedule(DEFAULT_GROUP_ID, request.createdByUserId(), request.frequency(),
                request.dayValue(), request.runMode(), members, LocalDate.now());
    }

    // Runs one schedule's rule today without moving its next date.
    @PostMapping("/{id}/run-now")
    public SettlementScheduleRun runNow(@PathVariable Long id) {
        return schedulerService.runNow(id, LocalDate.now());
    }

    // Runs every schedule that is due today or was missed.
    @PostMapping("/run-due")
    public List<SettlementScheduleRun> runDue() {
        return schedulerService.runDueSchedules(LocalDate.now());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        schedulerService.deleteSchedule(id);
    }

    @PostMapping("/reminders/{id}/done")
    public void reminderDone(@PathVariable Long id) {
        schedulerService.markReminderDone(id);
    }
}
