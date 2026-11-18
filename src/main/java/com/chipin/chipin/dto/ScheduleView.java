package com.chipin.chipin.dto;

import com.chipin.chipin.model.SettlementSchedule;
import com.chipin.chipin.service.SettlementDateCalculator;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

// A settlement schedule with the extra details the page needs (Feature 4).
public class ScheduleView {

    private final SettlementSchedule schedule;
    private final String description;
    private final long daysUntil;
    private final boolean dueNow;

    public ScheduleView(SettlementSchedule schedule, LocalDate today) {
        this.schedule = schedule;
        this.description = SettlementDateCalculator.describe(schedule.getFrequency(), schedule.getDayValue());
        this.daysUntil = schedule.getNextRunDate() == null ? 0
                : ChronoUnit.DAYS.between(today, schedule.getNextRunDate());
        this.dueNow = schedule.isActive() && SettlementDateCalculator.isDue(schedule.getNextRunDate(), today);
    }

    public Long getId() { return schedule.getId(); }
    public String getDescription() { return description; }
    public boolean isActive() { return schedule.isActive(); }
    public boolean isAuto() { return SettlementSchedule.MODE_AUTO.equals(schedule.getRunMode()); }
    public LocalDate getNextRunDate() { return schedule.getNextRunDate(); }
    public LocalDate getLastRunDate() { return schedule.getLastRunDate(); }
    public long getDaysUntil() { return daysUntil; }
    public boolean isDueNow() { return dueNow; }
    public String getCreatedByUserId() { return schedule.getCreatedByUserId(); }

    public List<String> getMembers() {
        return Arrays.asList(schedule.getMembersCsv().split(","));
    }
}
