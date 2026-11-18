package com.chipin.chipin.service;

import com.chipin.chipin.model.SettlementScheduleRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.util.List;

// Feature 4: the clock. Checks for due settlement schedules every day at 02:00, and once when the
// app starts so a schedule that came due while the app was off is not forgotten.
// The time can be changed with the property chipin.settlement-scheduler.cron.
@Configuration
@EnableScheduling
public class SettlementSchedulerJob {

    private static final Logger log = LoggerFactory.getLogger(SettlementSchedulerJob.class);

    private final SettlementSchedulerService schedulerService;

    public SettlementSchedulerJob(SettlementSchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    @Scheduled(cron = "${chipin.settlement-scheduler.cron:0 0 2 * * *}")
    public void runDaily() {
        runDue("daily check");
    }

    @EventListener(ContextRefreshedEvent.class)
    public void runAtStartup() {
        runDue("startup check");
    }

    private void runDue(String reason) {
        try {
            List<SettlementScheduleRun> runs = schedulerService.runDueSchedules(LocalDate.now());
            if (!runs.isEmpty()) {
                log.info("Settlement scheduler ({}): ran {} schedule(s).", reason, runs.size());
            }
        } catch (RuntimeException e) {
            // A problem here must never stop the app from starting.
            log.warn("Settlement scheduler ({}) could not run: {}", reason, e.getMessage());
        }
    }
}
