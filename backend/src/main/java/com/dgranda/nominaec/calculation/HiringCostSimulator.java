package com.dgranda.nominaec.calculation;

import com.dgranda.nominaec.entity.PaymentMode;
import com.dgranda.nominaec.entity.ProvisionType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * "How much does it cost to hire someone?" Reuses the payroll engine on a synthetic full month,
 * once for the first year (no reserve fund) and once from the second year on.
 */
public final class HiringCostSimulator {

    private static final BigDecimal MONTHS_PER_YEAR = BigDecimal.valueOf(12);

    private HiringCostSimulator() {
    }

    public record Input(BigDecimal monthlySalary, int weeklyHours, YearMonth referenceMonth,
                        PaymentMode thirteenthMode, PaymentMode fourteenthMode) {
    }

    public record Scenario(BigDecimal salary, BigDecimal employerIess, BigDecimal iece, BigDecimal secap,
                           BigDecimal thirteenth, BigDecimal fourteenth, BigDecimal reserveFund, BigDecimal vacation,
                           BigDecimal monthlyCost, BigDecimal annualCost, BigDecimal costOverSalary,
                           BigDecimal employeeIess, BigDecimal employeeIncomeTax, BigDecimal employeeNetMonthly) {
    }

    public record Result(BigDecimal sbu, boolean belowMinimumWage, Scenario firstYear, Scenario fromSecondYear) {
    }

    public static Result simulate(Input in, LegalParameterSet p) {
        Scenario first = scenario(in, p, in.referenceMonth().atDay(1));
        Scenario second = scenario(in, p, in.referenceMonth().atDay(1).minusYears(2));
        BigDecimal minimum = p.get(ParameterCode.SBU).multiply(
                Money.divide(BigDecimal.valueOf(in.weeklyHours()), p.get(ParameterCode.FULL_TIME_WEEKLY_HOURS)));
        return new Result(p.get(ParameterCode.SBU), in.monthlySalary().compareTo(Money.round(minimum)) < 0, first, second);
    }

    private static Scenario scenario(Input in, LegalParameterSet p, LocalDate contractStart) {
        PayrollInput payroll = new PayrollInput(in.referenceMonth(), in.monthlySalary(), in.weeklyHours(), contractStart, null,
                in.thirteenthMode(), in.fourteenthMode(), PaymentMode.MONTHLY,
                null, null, null, null, null, null, null, 0, false, null, null);
        PayrollResult r = PayrollCalculator.calculate(payroll, p);
        BigDecimal monthlyCost = r.employerCost();
        return new Scenario(
                r.amountOf("SALARY"),
                r.amountOf("IESS_EMPLOYER"),
                r.amountOf("IECE"),
                r.amountOf("SECAP"),
                r.provisionOf(ProvisionType.THIRTEENTH),
                r.provisionOf(ProvisionType.FOURTEENTH),
                r.provisionOf(ProvisionType.RESERVE_FUND),
                r.provisionOf(ProvisionType.VACATION),
                monthlyCost,
                Money.round(monthlyCost.multiply(MONTHS_PER_YEAR)),
                Money.divide(monthlyCost, in.monthlySalary()).setScale(4, RoundingMode.HALF_UP),
                r.amountOf("IESS_PERSONAL"),
                r.amountOf("INCOME_TAX"),
                r.netPay());
    }
}
