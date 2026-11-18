package com.chipin.chipin.dto;

import com.chipin.chipin.model.SettlementReminder;
import com.chipin.chipin.model.SettlementScheduleRun;

import java.math.BigDecimal;
import java.util.List;

// Everything the Settlement Scheduler page shows (Feature 4).
public class SchedulerOverview {

    private final List<ScheduleView> schedules;
    private final List<SettlementReminder> openReminders;
    private final List<SettlementScheduleRun> recentRuns;
    private final List<DebtEdge> paymentsNeededNow; // what a settlement would pay today

    public SchedulerOverview(List<ScheduleView> schedules, List<SettlementReminder> openReminders,
                             List<SettlementScheduleRun> recentRuns, List<DebtEdge> paymentsNeededNow) {
        this.schedules = schedules;
        this.openReminders = openReminders;
        this.recentRuns = recentRuns;
        this.paymentsNeededNow = paymentsNeededNow;
    }

    public List<ScheduleView> getSchedules() { return schedules; }
    public List<SettlementReminder> getOpenReminders() { return openReminders; }
    public List<SettlementScheduleRun> getRecentRuns() { return recentRuns; }
    public List<DebtEdge> getPaymentsNeededNow() { return paymentsNeededNow; }
    public boolean isSettledUp() { return paymentsNeededNow.isEmpty(); }

    public BigDecimal getTotalNeededNow() {
        BigDecimal total = BigDecimal.ZERO;
        for (DebtEdge edge : paymentsNeededNow) {
            total = total.add(edge.getAmount());
        }
        return total;
    }

    public boolean hasDueSchedule() {
        for (ScheduleView s : schedules) {
            if (s.isDueNow()) {
                return true;
            }
        }
        return false;
    }
}
