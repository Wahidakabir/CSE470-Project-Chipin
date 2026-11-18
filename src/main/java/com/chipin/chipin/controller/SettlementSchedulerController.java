package com.chipin.chipin.controller;

import com.chipin.chipin.model.SettlementSchedule;
import com.chipin.chipin.model.SettlementScheduleRun;
import com.chipin.chipin.service.SettlementDateCalculator;
import com.chipin.chipin.service.SettlementSchedulerService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

// Feature 4: Settlement Scheduler. Settles the group's balances on a schedule, such as the end of each month.
@Controller
@RequestMapping("/settlement-scheduler")
public class SettlementSchedulerController {

    private static final String DEFAULT_GROUP_ID = "group_01";
    private static final List<String> DEMO_MEMBERS = Arrays.asList("Afifa", "Mufliha", "Musarrat", "Sayma");

    private final SettlementSchedulerService schedulerService;

    public SettlementSchedulerController(SettlementSchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @GetMapping
    public String viewSchedulerPage(@RequestParam(defaultValue = "Mufliha") String currentUser, Model model) {
        LocalDate today = LocalDate.now();
        model.addAttribute("overview", schedulerService.getOverview(DEFAULT_GROUP_ID, DEMO_MEMBERS, today));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("members", DEMO_MEMBERS);
        model.addAttribute("today", today);
        return "settlement-scheduler";
    }

    @PostMapping("/create")
    public String create(@RequestParam String currentUser,
                         @RequestParam String frequency,
                         @RequestParam(defaultValue = "5") int weekday,
                         @RequestParam(defaultValue = "0") int monthDay,
                         @RequestParam String runMode,
                         RedirectAttributes redirectAttributes) {
        return respond(currentUser, redirectAttributes, () -> {
            int day = SettlementDateCalculator.WEEKLY.equals(frequency) ? weekday : monthDay;
            SettlementSchedule created = schedulerService.createSchedule(DEFAULT_GROUP_ID, currentUser,
                    frequency, day, runMode, DEMO_MEMBERS, LocalDate.now());
            return "Schedule saved. " + SettlementDateCalculator.describe(frequency, day)
                    + ", first on " + created.getNextRunDate() + ".";
        });
    }

    @PostMapping("/{id}/run-now")
    public String runNow(@PathVariable Long id, @RequestParam String currentUser,
                         RedirectAttributes redirectAttributes) {
        return respond(currentUser, redirectAttributes, () -> {
            SettlementScheduleRun run = schedulerService.runNow(id, LocalDate.now());
            return run.getNote();
        });
    }

    @PostMapping("/run-due")
    public String runDue(@RequestParam String currentUser, RedirectAttributes redirectAttributes) {
        return respond(currentUser, redirectAttributes, () -> {
            List<SettlementScheduleRun> runs = schedulerService.runDueSchedules(LocalDate.now());
            return runs.isEmpty() ? "Nothing is due today." : "Ran " + runs.size() + " due schedule(s).";
        });
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, @RequestParam boolean active, @RequestParam String currentUser,
                         RedirectAttributes redirectAttributes) {
        return respond(currentUser, redirectAttributes, () -> {
            schedulerService.setActive(id, active, LocalDate.now());
            return active ? "Schedule resumed." : "Schedule paused.";
        });
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @RequestParam String currentUser,
                         RedirectAttributes redirectAttributes) {
        return respond(currentUser, redirectAttributes, () -> {
            schedulerService.deleteSchedule(id);
            return "Schedule deleted.";
        });
    }

    @PostMapping("/reminders/{id}/done")
    public String reminderDone(@PathVariable Long id, @RequestParam String currentUser,
                               RedirectAttributes redirectAttributes) {
        return respond(currentUser, redirectAttributes, () -> {
            schedulerService.markReminderDone(id);
            return "Reminder marked as done.";
        });
    }

    // Runs an action, then comes back to the page with a success or error message.
    private String respond(String currentUser, RedirectAttributes redirectAttributes, Supplier<String> action) {
        try {
            redirectAttributes.addFlashAttribute("successMessage", action.get());
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/settlement-scheduler?currentUser=" + currentUser;
    }
}
