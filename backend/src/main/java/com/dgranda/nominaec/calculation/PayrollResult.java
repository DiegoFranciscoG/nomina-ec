package com.dgranda.nominaec.calculation;

import com.dgranda.nominaec.entity.ProvisionType;

import java.math.BigDecimal;
import java.util.List;

/** Output of the payroll engine for one employee and one month. All amounts rounded to cents. */
public record PayrollResult(
        BigDecimal workedDays,
        BigDecimal baseSalary,
        BigDecimal iessBase,
        BigDecimal incomeTaxBase,
        BigDecimal projectedAnnualTaxBase,
        List<Line> lines,
        List<Provision> provisions,
        BigDecimal totalIncome,
        BigDecimal totalDeductions,
        BigDecimal netPay,
        BigDecimal employerCost) {

    public BigDecimal amountOf(String conceptCode) {
        return lines.stream().filter(l -> l.conceptCode().equals(conceptCode))
                .map(Line::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal provisionOf(ProvisionType type) {
        return provisions.stream().filter(p -> p.type() == type)
                .map(Provision::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** One payslip line; {@code quantity} and {@code rate} are informative (hours, days, percentage). */
    public record Line(String conceptCode, LineKind kind, BigDecimal quantity, BigDecimal rate, BigDecimal amount) {
    }

    public record Provision(ProvisionType type, BigDecimal amount, boolean paidMonthly) {
    }

    public enum LineKind { INCOME, DEDUCTION, PROVISION, EMPLOYER_CONTRIBUTION }
}
