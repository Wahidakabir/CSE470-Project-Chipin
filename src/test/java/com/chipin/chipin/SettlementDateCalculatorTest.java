package com.chipin.chipin;

import com.chipin.chipin.service.SettlementDateCalculator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

// Feature 4: working out when a schedule runs next. Pure date maths, no database.
class SettlementDateCalculatorTest {

    @Test
    void weeklyPicksTheNextMatchingWeekdayAfterToday() {
        // 18 Nov 2026 is a Wednesday; Friday is day 5
        assertEquals(LocalDate.of(2026, 11, 20),
                SettlementDateCalculator.nextAfter("WEEKLY", 5, LocalDate.of(2026, 11, 18)));
    }

    @Test
    void weeklyOnTheSameWeekdayWaitsAFullWeek() {
        assertEquals(LocalDate.of(2026, 11, 25),
                SettlementDateCalculator.nextAfter("WEEKLY", 3, LocalDate.of(2026, 11, 18)));
        assertEquals(LocalDate.of(2026, 11, 18),
                SettlementDateCalculator.nextOnOrAfter("WEEKLY", 3, LocalDate.of(2026, 11, 18)));
    }

    @Test
    void monthlyUsesTheChosenDay() {
        assertEquals(LocalDate.of(2026, 11, 25),
                SettlementDateCalculator.nextAfter("MONTHLY", 25, LocalDate.of(2026, 11, 18)));
        assertEquals(LocalDate.of(2026, 12, 5),
                SettlementDateCalculator.nextAfter("MONTHLY", 5, LocalDate.of(2026, 11, 18)));
    }

    @Test
    void shortMonthsUseTheirLastDay() {
        assertEquals(LocalDate.of(2027, 2, 28),
                SettlementDateCalculator.nextAfter("MONTHLY", 31, LocalDate.of(2027, 2, 1)));
        assertEquals(LocalDate.of(2028, 2, 29), // leap year
                SettlementDateCalculator.nextAfter("MONTHLY", 31, LocalDate.of(2028, 2, 1)));
    }

    @Test
    void dayZeroMeansLastDayOfTheMonth() {
        assertEquals(LocalDate.of(2026, 11, 30),
                SettlementDateCalculator.nextAfter("MONTHLY", 0, LocalDate.of(2026, 11, 18)));
        assertEquals(LocalDate.of(2026, 12, 31),
                SettlementDateCalculator.nextAfter("MONTHLY", 0, LocalDate.of(2026, 11, 30)));
    }

    @Test
    void yearEndRollsOver() {
        assertEquals(LocalDate.of(2027, 1, 10),
                SettlementDateCalculator.nextAfter("MONTHLY", 10, LocalDate.of(2026, 12, 20)));
    }

    @Test
    void dueMeansTodayOrEarlier() {
        LocalDate today = LocalDate.of(2026, 11, 18);
        assertTrue(SettlementDateCalculator.isDue(today, today));
        assertTrue(SettlementDateCalculator.isDue(today.minusDays(3), today));
        assertFalse(SettlementDateCalculator.isDue(today.plusDays(1), today));
        assertFalse(SettlementDateCalculator.isDue(null, today));
    }

    @Test
    void rejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> SettlementDateCalculator.validate("DAILY", 1));
        assertThrows(IllegalArgumentException.class, () -> SettlementDateCalculator.validate("WEEKLY", 0));
        assertThrows(IllegalArgumentException.class, () -> SettlementDateCalculator.validate("WEEKLY", 8));
        assertThrows(IllegalArgumentException.class, () -> SettlementDateCalculator.validate("MONTHLY", 32));
        assertThrows(IllegalArgumentException.class, () -> SettlementDateCalculator.validate("MONTHLY", -1));
        assertDoesNotThrow(() -> SettlementDateCalculator.validate("MONTHLY", 0));
    }

    @Test
    void describesSchedulesInPlainWords() {
        assertEquals("Every Friday", SettlementDateCalculator.describe("WEEKLY", 5));
        assertEquals("Last day of every month", SettlementDateCalculator.describe("MONTHLY", 0));
        assertEquals("Day 15 of every month", SettlementDateCalculator.describe("MONTHLY", 15));
    }
}
