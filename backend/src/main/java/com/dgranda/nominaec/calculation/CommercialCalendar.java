package com.dgranda.nominaec.calculation;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Commercial calendar used by Ecuadorian payroll practice: every month has 30 days and a year 360.
 * The last day of any month (28, 29 or 31) counts as day 30.
 */
public final class CommercialCalendar {

    public static final int MONTH_DAYS = 30;
    public static final int YEAR_DAYS = 360;

    private CommercialCalendar() {
    }

    public static int commercialDay(LocalDate date) {
        if (date.getDayOfMonth() == date.lengthOfMonth()) {
            return MONTH_DAYS;
        }
        return Math.min(date.getDayOfMonth(), MONTH_DAYS);
    }

    /** Days between two dates, both inclusive, on a 30/360 basis. */
    public static int daysInclusive(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            return 0;
        }
        int days = (to.getYear() - from.getYear()) * YEAR_DAYS
                + (to.getMonthValue() - from.getMonthValue()) * MONTH_DAYS
                + (commercialDay(to) - commercialDay(from));
        return days + 1;
    }

    /** Days of the month covered by a contract that runs from {@code start} to {@code end} (nullable). */
    public static int daysCoveredInMonth(YearMonth month, LocalDate start, LocalDate end) {
        LocalDate first = month.atDay(1);
        LocalDate last = month.atEndOfMonth();
        LocalDate from = start.isAfter(first) ? start : first;
        LocalDate to = end != null && end.isBefore(last) ? end : last;
        if (to.isBefore(from)) {
            return 0;
        }
        return commercialDay(to) - (from.equals(first) ? 1 : commercialDay(from)) + 1;
    }
}
