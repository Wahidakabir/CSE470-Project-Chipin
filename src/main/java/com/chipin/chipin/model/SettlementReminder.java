package com.chipin.chipin.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

// A "please pay" reminder created by a settlement schedule in reminder mode (Feature 4).
@Entity
@Table(name = "settlement_reminders")
public class SettlementReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long scheduleId;
    private String groupId;
    private String fromUserId;  // the member who should pay
    private String toUserId;    // the member who should receive

    @Column(precision = 12, scale = 2)
    private BigDecimal amount;

    private LocalDate createdOn;
    private boolean done;

    public SettlementReminder() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getFromUserId() { return fromUserId; }
    public void setFromUserId(String fromUserId) { this.fromUserId = fromUserId; }
    public String getToUserId() { return toUserId; }
    public void setToUserId(String toUserId) { this.toUserId = toUserId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getCreatedOn() { return createdOn; }
    public void setCreatedOn(LocalDate createdOn) { this.createdOn = createdOn; }
    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }
}
