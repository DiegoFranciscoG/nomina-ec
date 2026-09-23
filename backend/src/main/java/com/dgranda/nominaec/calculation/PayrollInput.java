package com.dgranda.nominaec.calculation;

import com.dgranda.nominaec.entity.PaymentMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Everything the payroll engine needs for one employee and one month.
 *
 * @param priorTaxableIncome taxable income (after IESS) paid in earlier months of the same year
 * @param priorWithheldTax   income tax already withheld in earlier months of the same year
 */
public record PayrollInput(
        YearMonth period,
        BigDecimal monthlySalary,
        int weeklyHours,
        LocalDate contractStart,
        LocalDate contractEnd,
        PaymentMode thirteenthMode,
        PaymentMode fourteenthMode,
        PaymentMode reserveFundMode,
        BigDecimal supplementaryHours,
        BigDecimal extraordinaryHours,
        BigDecimal absenceDays,
        BigDecimal bonus,
        BigDecimal advances,
        BigDecimal otherDeductions,
        BigDecimal projectedPersonalExpenses,
        int familyDependents,
        boolean catastrophicCondition,
        BigDecimal priorTaxableIncome,
        BigDecimal priorWithheldTax) {

    public PayrollInput {
        supplementaryHours = orZero(supplementaryHours);
        extraordinaryHours = orZero(extraordinaryHours);
        absenceDays = orZero(absenceDays);
        bonus = orZero(bonus);
        advances = orZero(advances);
        otherDeductions = orZero(otherDeductions);
        projectedPersonalExpenses = orZero(projectedPersonalExpenses);
        priorTaxableIncome = orZero(priorTaxableIncome);
        priorWithheldTax = orZero(priorWithheldTax);
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
