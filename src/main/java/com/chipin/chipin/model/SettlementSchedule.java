package com.chipin.chipin.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

// A rule that settles a group's balances on a regular date (Feature 4).
@Entity
@Table(name = "settlement_schedules")
public class SettlementSchedule {

    public static final String MODE_REMINDER = "REMINDER"; // members are reminded to pay
    public static final String MODE_AUTO = "AUTO";         // payments are recorded automatically

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String groupId;
    private String createdByUserId;

    private String frequency;   // WEEKLY or MONTHLY
    private int dayValue;       // see SettlementDateCalculator
    private String runMode;     // REMINDER or AUTO

    // Who is included, as a comma separated list.
    @Column(length = 500)
    private String membersCsv;

    private boolean active = true;
    private LocalDate nextRunDate;
    private LocalDate lastRunDate;
    private LocalDateTime createdAt;

    public SettlementSchedule() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(String createdByUserId) { this.createdByUserId = createdByUserId; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public int getDayValue() { return dayValue; }
    public void setDayValue(int dayValue) { this.dayValue = dayValue; }
    public String getRunMode() { return runMode; }
    public void setRunMode(String runMode) { this.runMode = runMode; }
    public String getMembersCsv() { return membersCsv; }
    public void setMembersCsv(String membersCsv) { this.membersCsv = membersCsv; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDate getNextRunDate() { return nextRunDate; }
    public void setNextRunDate(LocalDate nextRunDate) { this.nextRunDate = nextRunDate; }
    public LocalDate getLastRunDate() { return lastRunDate; }
    public void setLastRunDate(LocalDate lastRunDate) { this.lastRunDate = lastRunDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
