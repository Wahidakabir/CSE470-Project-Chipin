package com.chipin.chipin.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

// Feature 4: works out when a settlement schedule runs next. No database, so it is easy to test.
//
// A schedule is a frequency plus a day:
//   WEEKLY  + day 1..7   -> every Monday..Sunday (1 = Monday, 7 = Sunday)
//   MONTHLY + day 1..31  -> that day of the month; a short month uses its last day instead
//   MONTHLY + day 0      -> the last day of every month ("end of the month")
public final class SettlementDateCalculator {

    public static final String WEEKLY = "WEEKLY";
    public static final String MONTHLY = "MONTHLY";
    public static final int LAST_DAY_OF_MONTH = 0;

    private SettlementDateCalculator() {}

    // Throws IllegalArgumentException with a message the person can read.
    public static void validate(String frequency, int day) {
        if (WEEKLY.equals(frequency)) {
            if (day < 1 || day > 7) {
                throw new IllegalArgumentException("Choose a day of the week.");
            }
        } else if (MONTHLY.equals(frequency)) {
            if (day < LAST_DAY_OF_MONTH || day > 31) {
                throw new IllegalArgumentException("Choose a day of the month between 1 and 31, or the last day.");
            }
        } else {
            throw new IllegalArgumentException("Frequency must be weekly or monthly.");
        }
    }

    // The first matching date that is the same as or later than "from".
    public static LocalDate nextOnOrAfter(String frequency, int day, LocalDate from) {
        validate(frequency, day);
        if (WEEKLY.equals(frequency)) {
            return from.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(day)));
        }
        LocalDate thisMonth = monthlyDate(YearMonth.from(from), day);
        if (!thisMonth.isBefore(from)) {
            return thisMonth;
        }
        return monthlyDate(YearMonth.from(from).plusMonths(1), day);
    }

    // The first matching date after "after" (never the same day).
    public static LocalDate nextAfter(String frequency, int day, LocalDate after) {
        return nextOnOrAfter(frequency, day, after.plusDays(1));
    }

    public static boolean isDue(LocalDate nextRunDate, LocalDate today) {
        return nextRunDate != null && !nextRunDate.isAfter(today);
    }

    // Plain-language description, for example "Every Friday" or "Last day of every month".
    public static String describe(String frequency, int day) {
        validate(frequency, day);
        if (WEEKLY.equals(frequency)) {
            return "Every " + DayOfWeek.of(day).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        }
        if (day == LAST_DAY_OF_MONTH) {
            return "Last day of every month";
        }
        return "Day " + day + " of every month";
    }

    // A day that does not exist in a short month (the 31st in April) falls back to the last day.
    private static LocalDate monthlyDate(YearMonth month, int day) {
        int dayOfMonth = (day == LAST_DAY_OF_MONTH) ? month.lengthOfMonth() : Math.min(day, month.lengthOfMonth());
        return month.atDay(dayOfMonth);
    }
}
