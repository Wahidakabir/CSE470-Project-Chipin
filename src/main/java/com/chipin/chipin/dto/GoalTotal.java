package com.chipin.chipin.dto;

import java.math.BigDecimal;

// How much round-up money has gone into one savings goal.
public class GoalTotal {

    private final String name;
    private final String type;      // PERSONAL or GROUP
    private final BigDecimal total;
    private final int payments;     // number of round-ups that fed this goal
    private final int barWidth;     // 0 to 100, relative to the biggest goal shown

    public GoalTotal(String name, String type, BigDecimal total, int payments, int barWidth) {
        this.name = name;
        this.type = type;
        this.total = total;
        this.payments = payments;
        this.barWidth = barWidth;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public BigDecimal getTotal() { return total; }
    public int getPayments() { return payments; }
    public int getBarWidth() { return barWidth; }
    public boolean isGroup() { return "GROUP".equals(type); }
}
