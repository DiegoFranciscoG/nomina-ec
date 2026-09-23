package com.dgranda.nominaec.calculation;

import java.math.BigDecimal;

import static com.dgranda.nominaec.calculation.ParameterCode.BASIC_FAMILY_BASKET;
import static com.dgranda.nominaec.calculation.ParameterCode.PERSONAL_EXPENSE_CATASTROPHIC_BASKETS;
import static com.dgranda.nominaec.calculation.ParameterCode.PERSONAL_EXPENSE_REBATE_RATE;

/**
 * Income tax for employment income (SRI table + personal expense rebate), and the monthly
 * withholding that spreads the pending annual tax over the remaining months of the year.
 */
public final class IncomeTaxCalculator {

    private IncomeTaxCalculator() {
    }

    /** Annual tax from the progressive table, before the personal expense rebate. Not rounded. */
    public static BigDecimal annualTax(BigDecimal annualTaxableBase, LegalParameterSet params) {
        if (annualTaxableBase.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        for (TaxBracket bracket : params.taxBrackets()) {
            if (bracket.contains(annualTaxableBase)) {
                BigDecimal excess = annualTaxableBase.subtract(bracket.lowerBound());
                return bracket.baseTax().add(excess.multiply(bracket.marginalRate()));
            }
        }
        throw new IllegalStateException("La tabla de IR no cubre la base " + annualTaxableBase);
    }

    /** Maximum deductible personal expenses: baskets x basic family basket value. */
    public static BigDecimal personalExpenseCap(int familyDependents, boolean catastrophicCondition, LegalParameterSet params) {
        BigDecimal baskets = catastrophicCondition
                ? params.get(PERSONAL_EXPENSE_CATASTROPHIC_BASKETS)
                : params.expenseCapBaskets(familyDependents);
        return baskets.multiply(params.get(BASIC_FAMILY_BASKET));
    }

    /** Rebate = rate x min(projected expenses, cap). Not rounded. */
    public static BigDecimal personalExpenseRebate(BigDecimal projectedExpenses, int familyDependents,
                                                   boolean catastrophicCondition, LegalParameterSet params) {
        if (projectedExpenses == null || projectedExpenses.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal cap = personalExpenseCap(familyDependents, catastrophicCondition, params);
        return Money.min(projectedExpenses, cap).multiply(params.get(PERSONAL_EXPENSE_REBATE_RATE));
    }

    /** Annual tax due after the rebate, never negative. Not rounded. */
    public static BigDecimal annualTaxDue(BigDecimal annualTaxableBase, BigDecimal projectedExpenses, int familyDependents,
                                          boolean catastrophicCondition, LegalParameterSet params) {
        BigDecimal tax = annualTax(annualTaxableBase, params)
                .subtract(personalExpenseRebate(projectedExpenses, familyDependents, catastrophicCondition, params));
        return Money.max(tax, BigDecimal.ZERO);
    }

    /**
     * Monthly withholding.
     *
     * @param monthlyTaxable   taxable income of the month (after the personal IESS contribution)
     * @param priorTaxable     taxable income already paid this year in previous months
     * @param priorWithheld    tax already withheld this year
     * @param month            1..12
     */
    public static WithholdingResult monthlyWithholding(BigDecimal monthlyTaxable, BigDecimal priorTaxable, BigDecimal priorWithheld,
                                                       int month, BigDecimal projectedExpenses, int familyDependents,
                                                       boolean catastrophicCondition, LegalParameterSet params) {
        BigDecimal remainingMonths = BigDecimal.valueOf(13L - month);
        BigDecimal projectedAnnual = Money.round(priorTaxable.add(monthlyTaxable.multiply(remainingMonths)));
        BigDecimal annualTaxDue = annualTaxDue(projectedAnnual, projectedExpenses, familyDependents, catastrophicCondition, params);
        BigDecimal pending = Money.max(annualTaxDue.subtract(priorWithheld), BigDecimal.ZERO);
        BigDecimal withholding = Money.round(Money.divide(pending, remainingMonths));
        return new WithholdingResult(projectedAnnual, Money.round(annualTaxDue), withholding);
    }

    public record WithholdingResult(BigDecimal projectedAnnualBase, BigDecimal annualTaxDue, BigDecimal monthlyWithholding) {
    }
}
