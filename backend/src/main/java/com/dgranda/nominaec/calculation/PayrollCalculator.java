package com.dgranda.nominaec.calculation;

import com.dgranda.nominaec.calculation.PayrollResult.Line;
import com.dgranda.nominaec.calculation.PayrollResult.LineKind;
import com.dgranda.nominaec.calculation.PayrollResult.Provision;
import com.dgranda.nominaec.entity.PaymentMode;
import com.dgranda.nominaec.entity.ProvisionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static com.dgranda.nominaec.calculation.Money.divide;
import static com.dgranda.nominaec.calculation.Money.round;
import static com.dgranda.nominaec.calculation.ParameterCode.*;

/**
 * Monthly payroll engine (Código del Trabajo, IESS and SRI rules; see docs/investigacion.md).
 * Pure function: same input + same parameter set = same result.
 */
public final class PayrollCalculator {

    private static final BigDecimal MONTH_DAYS = BigDecimal.valueOf(CommercialCalendar.MONTH_DAYS);

    private PayrollCalculator() {
    }

    public static PayrollResult calculate(PayrollInput in, LegalParameterSet p) {
        validate(in, p);
        List<Line> lines = new ArrayList<>();
        List<Provision> provisions = new ArrayList<>();

        // 1. Days and ordinary salary (30-day commercial month)
        int coveredDays = CommercialCalendar.daysCoveredInMonth(in.period(), in.contractStart(), in.contractEnd());
        BigDecimal workedDays = BigDecimal.valueOf(coveredDays).subtract(in.absenceDays()).max(BigDecimal.ZERO);
        BigDecimal dayFraction = divide(workedDays, MONTH_DAYS);
        BigDecimal salaryEarned = round(in.monthlySalary().multiply(dayFraction));
        lines.add(new Line("SALARY", LineKind.INCOME, workedDays, null, salaryEarned));

        // 2. Overtime (Art. 55): hourly value = salary / (240 h x weekly hours / 40)
        BigDecimal jornadaFactor = divide(BigDecimal.valueOf(in.weeklyHours()), p.get(FULL_TIME_WEEKLY_HOURS));
        BigDecimal hourlyRate = divide(in.monthlySalary(), p.get(MONTHLY_HOURS_BASE).multiply(jornadaFactor));
        BigDecimal supplementary = overtime(in.supplementaryHours(), hourlyRate, p.get(OVERTIME_SUPPLEMENTARY_SURCHARGE));
        BigDecimal extraordinary = overtime(in.extraordinaryHours(), hourlyRate, p.get(OVERTIME_EXTRAORDINARY_SURCHARGE));
        addIfPositive(lines, "OVERTIME_SUPPLEMENTARY", LineKind.INCOME, in.supplementaryHours(), round(hourlyRate), supplementary);
        addIfPositive(lines, "OVERTIME_EXTRAORDINARY", LineKind.INCOME, in.extraordinaryHours(), round(hourlyRate), extraordinary);
        BigDecimal bonus = round(in.bonus());
        addIfPositive(lines, "BONUS", LineKind.INCOME, null, null, bonus);

        // 3. Remuneration base for IESS and benefits (Art. 95)
        BigDecimal iessBase = salaryEarned.add(supplementary).add(extraordinary).add(bonus);
        BigDecimal iessPersonal = round(iessBase.multiply(p.get(IESS_PERSONAL_RATE)));

        // 4. Thirteenth (Art. 111), fourteenth (Art. 113), reserve fund (Art. 196), vacation (Art. 71)
        BigDecimal thirteenth = round(divide(iessBase, p.get(THIRTEENTH_DIVISOR)));
        benefit(lines, provisions, ProvisionType.THIRTEENTH, "THIRTEENTH_MONTHLY", in.thirteenthMode(), thirteenth);

        BigDecimal fourteenth = round(divide(p.get(SBU), p.get(FOURTEENTH_DIVISOR)).multiply(dayFraction).multiply(jornadaFactor));
        benefit(lines, provisions, ProvisionType.FOURTEENTH, "FOURTEENTH_MONTHLY", in.fourteenthMode(), fourteenth);

        BigDecimal reserveFactor = reserveFundFraction(in.period(), in.contractStart());
        BigDecimal reserveFund = round(iessBase.multiply(p.get(RESERVE_FUND_RATE)).multiply(reserveFactor));
        if (reserveFund.signum() > 0) {
            benefit(lines, provisions, ProvisionType.RESERVE_FUND, "RESERVE_FUND_MONTHLY", in.reserveFundMode(), reserveFund);
        }

        BigDecimal vacation = round(divide(iessBase, p.get(VACATION_PROVISION_DIVISOR)));
        provisions.add(new Provision(ProvisionType.VACATION, vacation, false));
        lines.add(new Line("PROV_VACATION", LineKind.PROVISION, null, null, vacation));

        // 5. Income tax withholding (LRTI Arts. 9, 17; Reglamento Art. 104)
        BigDecimal incomeTaxBase = iessBase.subtract(iessPersonal);
        IncomeTaxCalculator.WithholdingResult tax = IncomeTaxCalculator.monthlyWithholding(
                incomeTaxBase, in.priorTaxableIncome(), in.priorWithheldTax(), in.period().getMonthValue(),
                in.projectedPersonalExpenses(), in.familyDependents(), in.catastrophicCondition(), p);

        // 6. Deductions
        lines.add(new Line("IESS_PERSONAL", LineKind.DEDUCTION, null, p.get(IESS_PERSONAL_RATE), iessPersonal));
        addIfPositive(lines, "INCOME_TAX", LineKind.DEDUCTION, null, null, tax.monthlyWithholding());
        addIfPositive(lines, "ADVANCE", LineKind.DEDUCTION, null, null, round(in.advances()));
        addIfPositive(lines, "OTHER_DEDUCTION", LineKind.DEDUCTION, null, null, round(in.otherDeductions()));

        // 7. Employer contributions
        BigDecimal employerIess = round(iessBase.multiply(p.get(IESS_EMPLOYER_RATE)));
        BigDecimal iece = round(iessBase.multiply(p.get(IECE_RATE)));
        BigDecimal secap = round(iessBase.multiply(p.get(SECAP_RATE)));
        lines.add(new Line("IESS_EMPLOYER", LineKind.EMPLOYER_CONTRIBUTION, null, p.get(IESS_EMPLOYER_RATE), employerIess));
        lines.add(new Line("IECE", LineKind.EMPLOYER_CONTRIBUTION, null, p.get(IECE_RATE), iece));
        lines.add(new Line("SECAP", LineKind.EMPLOYER_CONTRIBUTION, null, p.get(SECAP_RATE), secap));

        // 8. Totals
        BigDecimal totalIncome = sum(lines, LineKind.INCOME);
        BigDecimal totalDeductions = sum(lines, LineKind.DEDUCTION);
        BigDecimal netPay = totalIncome.subtract(totalDeductions);
        if (netPay.signum() < 0) {
            throw new PayrollValidationException("Los descuentos superan los ingresos del mes (neto " + netPay + ")");
        }
        BigDecimal employerCost = totalIncome
                .add(sum(lines, LineKind.EMPLOYER_CONTRIBUTION))
                .add(sum(lines, LineKind.PROVISION));

        return new PayrollResult(workedDays, in.monthlySalary(), iessBase, incomeTaxBase, tax.projectedAnnualBase(),
                List.copyOf(lines), List.copyOf(provisions), totalIncome, totalDeductions, netPay, employerCost);
    }

    /**
     * Share of the month that generates reserve fund: 0 during the first year, 1 after it and
     * the proportional commercial days in the month where the first anniversary falls.
     */
    static BigDecimal reserveFundFraction(YearMonth period, LocalDate contractStart) {
        LocalDate eligibleFrom = contractStart.plusYears(1);
        if (!eligibleFrom.isAfter(period.atDay(1))) {
            return BigDecimal.ONE;
        }
        if (eligibleFrom.isAfter(period.atEndOfMonth())) {
            return BigDecimal.ZERO;
        }
        int days = CommercialCalendar.daysCoveredInMonth(period, eligibleFrom, null);
        return divide(BigDecimal.valueOf(days), MONTH_DAYS);
    }

    private static void validate(PayrollInput in, LegalParameterSet p) {
        if (in.monthlySalary() == null || in.monthlySalary().signum() <= 0) {
            throw new PayrollValidationException("El sueldo debe ser mayor a cero");
        }
        if (in.supplementaryHours().compareTo(p.get(OVERTIME_SUPPLEMENTARY_MAX_MONTH)) > 0) {
            throw new PayrollValidationException("Las horas suplementarias superan el máximo mensual de "
                    + p.get(OVERTIME_SUPPLEMENTARY_MAX_MONTH).stripTrailingZeros().toPlainString() + " h (Art. 55)");
        }
        if (in.absenceDays().compareTo(MONTH_DAYS) > 0) {
            throw new PayrollValidationException("Las faltas no pueden superar 30 días");
        }
    }

    private static BigDecimal overtime(BigDecimal hours, BigDecimal hourlyRate, BigDecimal surcharge) {
        return round(hours.multiply(hourlyRate).multiply(BigDecimal.ONE.add(surcharge)));
    }

    private static void benefit(List<Line> lines, List<Provision> provisions, ProvisionType type,
                                String monthlyConcept, PaymentMode mode, BigDecimal amount) {
        boolean monthly = mode == PaymentMode.MONTHLY;
        provisions.add(new Provision(type, amount, monthly));
        if (monthly) {
            lines.add(new Line(monthlyConcept, LineKind.INCOME, null, null, amount));
        } else {
            lines.add(new Line("PROV_" + type.name(), LineKind.PROVISION, null, null, amount));
        }
    }

    private static void addIfPositive(List<Line> lines, String code, LineKind kind, BigDecimal qty, BigDecimal rate, BigDecimal amount) {
        if (amount.signum() > 0) {
            lines.add(new Line(code, kind, qty, rate, amount));
        }
    }

    private static BigDecimal sum(List<Line> lines, LineKind kind) {
        return lines.stream().filter(l -> l.kind() == kind).map(Line::amount).reduce(Money.ZERO, BigDecimal::add);
    }
}
