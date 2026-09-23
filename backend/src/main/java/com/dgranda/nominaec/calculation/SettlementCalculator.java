package com.dgranda.nominaec.calculation;

import com.dgranda.nominaec.entity.PaymentMode;
import com.dgranda.nominaec.entity.Region;
import com.dgranda.nominaec.entity.SettlementReason;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.dgranda.nominaec.calculation.Money.divide;
import static com.dgranda.nominaec.calculation.Money.round;
import static com.dgranda.nominaec.calculation.ParameterCode.*;

/**
 * Basic final settlement ("liquidación de haberes"): pending salary, proportional thirteenth and
 * fourteenth, vacation, severance bonus (Art. 185) and unjustified dismissal indemnity (Art. 188).
 */
public final class SettlementCalculator {

    private static final BigDecimal MONTH_DAYS = BigDecimal.valueOf(CommercialCalendar.MONTH_DAYS);
    private static final BigDecimal YEAR_DAYS = BigDecimal.valueOf(CommercialCalendar.YEAR_DAYS);

    private SettlementCalculator() {
    }

    /**
     * @param accumulatedThirteenth thirteenth already provisioned and not paid in the current cycle
     * @param pendingSalaryDays     days of the last month not yet paid through payroll
     * @param unusedVacationDays    vacation days from previous service years not enjoyed
     */
    public record Input(BigDecimal lastSalary, int weeklyHours, LocalDate startDate, LocalDate terminationDate,
                        SettlementReason reason, Region region, PaymentMode thirteenthMode, PaymentMode fourteenthMode,
                        BigDecimal accumulatedThirteenth, BigDecimal pendingSalaryDays, BigDecimal unusedVacationDays) {
    }

    public record Item(String code, String description, BigDecimal amount) {
    }

    public record Result(int serviceDays, BigDecimal serviceYears, List<Item> items, BigDecimal totalIncome,
                         BigDecimal totalDeductions, BigDecimal netTotal) {
    }

    public static Result calculate(Input in, LegalParameterSet p) {
        if (in.terminationDate().isBefore(in.startDate())) {
            throw new PayrollValidationException("La fecha de salida es anterior a la fecha de ingreso");
        }
        List<Item> incomes = new ArrayList<>();
        int serviceDays = CommercialCalendar.daysInclusive(in.startDate(), in.terminationDate());
        BigDecimal serviceYears = divide(BigDecimal.valueOf(serviceDays), YEAR_DAYS);
        BigDecimal salary = in.lastSalary();
        BigDecimal jornadaFactor = divide(BigDecimal.valueOf(in.weeklyHours()), p.get(FULL_TIME_WEEKLY_HOURS));

        BigDecimal pendingDays = orZero(in.pendingSalaryDays());
        BigDecimal pendingSalary = round(salary.multiply(divide(pendingDays, MONTH_DAYS)));
        add(incomes, "PENDING_SALARY", "Sueldo pendiente (" + pendingDays.stripTrailingZeros().toPlainString() + " días)", pendingSalary);

        // Thirteenth: what is accumulated plus the share of the unpaid salary
        BigDecimal thirteenth = divide(pendingSalary, p.get(THIRTEENTH_DIVISOR));
        if (in.thirteenthMode() == PaymentMode.ACCUMULATED) {
            thirteenth = thirteenth.add(orZero(in.accumulatedThirteenth()));
        }
        add(incomes, "THIRTEENTH", "Décimo tercero proporcional (Art. 111)", round(thirteenth));

        // Fourteenth: SBU x days of the regional cycle / 360 (Art. 113)
        BigDecimal fourteenth;
        if (in.fourteenthMode() == PaymentMode.ACCUMULATED) {
            LocalDate cycleStart = fourteenthCycleStart(in.region(), in.terminationDate());
            LocalDate from = in.startDate().isAfter(cycleStart) ? in.startDate() : cycleStart;
            int cycleDays = CommercialCalendar.daysInclusive(from, in.terminationDate());
            fourteenth = p.get(SBU).multiply(divide(BigDecimal.valueOf(cycleDays), YEAR_DAYS));
        } else {
            fourteenth = divide(p.get(SBU), p.get(FOURTEENTH_DIVISOR)).multiply(divide(pendingDays, MONTH_DAYS));
        }
        add(incomes, "FOURTEENTH", "Décimo cuarto proporcional (Art. 113)", round(fourteenth.multiply(jornadaFactor)));

        // Vacation: proportional share of the current service year + unused days (Arts. 69 and 71)
        int completedYears = serviceDays / CommercialCalendar.YEAR_DAYS;
        LocalDate lastAnniversary = in.startDate().plusYears(completedYears);
        int daysSinceAnniversary = CommercialCalendar.daysInclusive(lastAnniversary, in.terminationDate());
        BigDecimal entitledDays = vacationDaysForYear(completedYears + 1, p);
        BigDecimal dailySalary = divide(salary, MONTH_DAYS);
        BigDecimal proportionalVacation = dailySalary.multiply(entitledDays)
                .multiply(divide(BigDecimal.valueOf(daysSinceAnniversary), YEAR_DAYS));
        add(incomes, "VACATION_PROPORTIONAL", "Vacaciones proporcionales (Art. 71)", round(proportionalVacation));
        add(incomes, "VACATION_UNUSED", "Vacaciones no gozadas", round(dailySalary.multiply(orZero(in.unusedVacationDays()))));

        // Severance bonus (Art. 185): resignation, mutual agreement and unjustified dismissal
        if (in.reason() != SettlementReason.JUSTIFIED_DISMISSAL) {
            BigDecimal years = p.get(SEVERANCE_PRORATE_FRACTION).signum() > 0
                    ? serviceYears
                    : BigDecimal.valueOf(completedYears);
            add(incomes, "SEVERANCE_BONUS", "Bonificación por desahucio 25 % (Art. 185)",
                    round(salary.multiply(p.get(SEVERANCE_BONUS_RATE)).multiply(years)));
        }

        // Unjustified dismissal indemnity (Art. 188): fraction of a year counts as a full year
        if (in.reason() == SettlementReason.UNJUSTIFIED_DISMISSAL) {
            int yearsRoundedUp = serviceYears.setScale(0, RoundingMode.CEILING).intValue();
            int months = yearsRoundedUp <= p.getInt(DISMISSAL_THRESHOLD_YEARS)
                    ? p.getInt(DISMISSAL_MIN_MONTHS)
                    : Math.min(yearsRoundedUp, p.getInt(DISMISSAL_MAX_MONTHS));
            add(incomes, "DISMISSAL_INDEMNITY", "Indemnización por despido intempestivo: " + months + " remuneraciones (Art. 188)",
                    round(salary.multiply(BigDecimal.valueOf(months))));
        }

        BigDecimal totalIncome = incomes.stream().map(Item::amount).reduce(Money.ZERO, BigDecimal::add);
        BigDecimal iessPersonal = round(pendingSalary.multiply(p.get(IESS_PERSONAL_RATE)));
        List<Item> items = new ArrayList<>(incomes);
        if (iessPersonal.signum() > 0) {
            items.add(new Item("IESS_PERSONAL", "Aporte personal IESS sobre sueldo pendiente", iessPersonal.negate()));
        }
        return new Result(serviceDays, serviceYears.setScale(4, RoundingMode.HALF_UP), List.copyOf(items),
                totalIncome, iessPersonal, totalIncome.subtract(iessPersonal));
    }

    /** Start of the fourteenth cycle containing {@code date}: Mar 1 (Costa/Galápagos) or Aug 1 (Sierra/Amazonía). */
    public static LocalDate fourteenthCycleStart(Region region, LocalDate date) {
        int startMonth = region == Region.COSTA_GALAPAGOS ? 3 : 8;
        int year = date.getMonthValue() >= startMonth ? date.getYear() : date.getYear() - 1;
        return LocalDate.of(year, startMonth, 1);
    }

    /** Vacation days for the n-th service year: 15, plus one per year after the fifth, capped (Art. 69). */
    public static BigDecimal vacationDaysForYear(int serviceYear, LegalParameterSet p) {
        int extra = serviceYear > p.getInt(VACATION_EXTRA_AFTER_YEARS)
                ? Math.min(serviceYear - p.getInt(VACATION_EXTRA_AFTER_YEARS), p.getInt(VACATION_EXTRA_MAX_DAYS))
                : 0;
        return p.get(VACATION_BASE_DAYS).add(BigDecimal.valueOf(extra));
    }

    private static void add(List<Item> items, String code, String description, BigDecimal amount) {
        if (amount.signum() > 0) {
            items.add(new Item(code, description, amount));
        }
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
