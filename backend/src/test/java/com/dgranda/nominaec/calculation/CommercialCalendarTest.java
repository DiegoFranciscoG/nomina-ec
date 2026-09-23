package com.dgranda.nominaec.calculation;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class CommercialCalendarTest {

    @Test
    void lastDayOfAnyMonthCountsAsThirty() {
        assertThat(CommercialCalendar.commercialDay(LocalDate.of(2026, 2, 28))).isEqualTo(30);
        assertThat(CommercialCalendar.commercialDay(LocalDate.of(2026, 1, 31))).isEqualTo(30);
        assertThat(CommercialCalendar.commercialDay(LocalDate.of(2026, 1, 30))).isEqualTo(30);
        assertThat(CommercialCalendar.commercialDay(LocalDate.of(2026, 1, 15))).isEqualTo(15);
    }

    @Test
    void fullYearIs360Days() {
        assertThat(CommercialCalendar.daysInclusive(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))).isEqualTo(360);
        assertThat(CommercialCalendar.daysInclusive(LocalDate.of(2023, 3, 1), LocalDate.of(2026, 2, 28))).isEqualTo(1080);
        assertThat(CommercialCalendar.daysInclusive(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1))).isZero();
    }

    @Test
    void daysCoveredInMonthHandlesStartAndEndInsideTheMonth() {
        YearMonth feb = YearMonth.of(2026, 2);
        assertThat(CommercialCalendar.daysCoveredInMonth(feb, LocalDate.of(2020, 1, 1), null)).isEqualTo(30);
        assertThat(CommercialCalendar.daysCoveredInMonth(feb, LocalDate.of(2026, 2, 16), null)).isEqualTo(15);
        assertThat(CommercialCalendar.daysCoveredInMonth(feb, LocalDate.of(2020, 1, 1), LocalDate.of(2026, 2, 10))).isEqualTo(10);
        assertThat(CommercialCalendar.daysCoveredInMonth(feb, LocalDate.of(2026, 3, 1), null)).isZero();
    }
}
