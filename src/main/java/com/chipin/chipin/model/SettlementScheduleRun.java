package com.chipin.chipin.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// One time a settlement schedule ran, kept as history (Feature 4).
@Entity
@Table(name = "settlement_schedule_runs")
public class SettlementScheduleRun {

    public static final String SCHEDULED = "SCHEDULED"; // ran on its date
    public static final String MANUAL = "MANUAL";       // someone pressed "Run now"

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long scheduleId;
    private String groupId;
    private LocalDate runDate;
    private String runMode;
    private String triggeredBy;

    private int paymentsMade;
    private int remindersSent;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 300)
    private String note;

    private LocalDateTime ranAt;

    public SettlementScheduleRun() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public LocalDate getRunDate() { return runDate; }
    public void setRunDate(LocalDate runDate) { this.runDate = runDate; }
    public String getRunMode() { return runMode; }
    public void setRunMode(String runMode) { this.runMode = runMode; }
    public String getTriggeredBy() { return triggeredBy; }
    public void setTriggeredBy(String triggeredBy) { this.triggeredBy = triggeredBy; }
    public int getPaymentsMade() { return paymentsMade; }
    public void setPaymentsMade(int paymentsMade) { this.paymentsMade = paymentsMade; }
    public int getRemindersSent() { return remindersSent; }
    public void setRemindersSent(int remindersSent) { this.remindersSent = remindersSent; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getRanAt() { return ranAt; }
    public void setRanAt(LocalDateTime ranAt) { this.ranAt = ranAt; }
}
