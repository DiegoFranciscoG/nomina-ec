package com.dgranda.nominaec.calculation;

import com.dgranda.nominaec.entity.PaymentMode;
import com.dgranda.nominaec.entity.ProvisionType;
import com.dgranda.nominaec.support.TestParameters;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

import static com.dgranda.nominaec.entity.PaymentMode.ACCUMULATED;
import static com.dgranda.nominaec.entity.PaymentMode.MONTHLY;
import static com.dgranda.nominaec.support.TestParameters.bd;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Hand-verified payroll cases. Each comment shows the arithmetic so that a reviewer can check it with a calculator.
 */
class PayrollCalculatorTest {

    private final LegalParameterSet params = TestParameters.y2026();

    private static PayrollInput input(String salary, YearMonth period, LocalDate start,
                                      PaymentMode thirteenth, PaymentMode fourteenth, PaymentMode reserve,
                                      String supHours, String extHours, String absence, String gp) {
        return new PayrollInput(period, bd(salary), 40, start, null, thirteenth, fourteenth, reserve,
                bd(supHours), bd(extHours), bd(absence), null, null, null, bd(gp), 0, false, null, null);
    }

    @Nested
    class CaseA_OvertimeAccumulatedBenefitsAndReserveFund {
        // Sueldo 1200, ingreso 2023-03-01 (> 1 año), 10 h al 50 %, 4 h al 100 %, décimos acumulados, FR mensualizado.
        private final PayrollResult r = PayrollCalculator.calculate(
                input("1200", YearMonth.of(2026, 1), LocalDate.of(2023, 3, 1), ACCUMULATED, ACCUMULATED, MONTHLY,
                        "10", "4", "0", "3000"), params);

        @Test
        void overtimeUsesHourlyValueOfSalaryOver240() {
            // valor hora = 1200 / 240 = 5.00
            assertThat(r.amountOf("SALARY")).isEqualByComparingTo("1200.00");
            assertThat(r.amountOf("OVERTIME_SUPPLEMENTARY")).isEqualByComparingTo("75.00");   // 10 x 5 x 1.5
            assertThat(r.amountOf("OVERTIME_EXTRAORDINARY")).isEqualByComparingTo("40.00");   // 4 x 5 x 2
            assertThat(r.iessBase()).isEqualByComparingTo("1315.00");
        }

        @Test
        void iessContributions() {
            assertThat(r.amountOf("IESS_PERSONAL")).isEqualByComparingTo("124.27");   // 1315 x 9.45 % = 124.2675
            assertThat(r.amountOf("IESS_EMPLOYER")).isEqualByComparingTo("146.62");   // 1315 x 11.15 % = 146.6225
            assertThat(r.amountOf("IECE")).isEqualByComparingTo("6.58");              // 1315 x 0.5 % = 6.575
            assertThat(r.amountOf("SECAP")).isEqualByComparingTo("6.58");
        }

        @Test
        void provisions() {
            assertThat(r.provisionOf(ProvisionType.THIRTEENTH)).isEqualByComparingTo("109.58");   // 1315 / 12
            assertThat(r.provisionOf(ProvisionType.FOURTEENTH)).isEqualByComparingTo("40.17");    // 482 / 12
            assertThat(r.provisionOf(ProvisionType.RESERVE_FUND)).isEqualByComparingTo("109.54"); // 1315 x 8.33 %
            assertThat(r.provisionOf(ProvisionType.VACATION)).isEqualByComparingTo("54.79");      // 1315 / 24
            assertThat(r.amountOf("PROV_THIRTEENTH")).isEqualByComparingTo("109.58");
            assertThat(r.amountOf("THIRTEENTH_MONTHLY")).isZero();
            assertThat(r.amountOf("RESERVE_FUND_MONTHLY")).isEqualByComparingTo("109.54");
        }

        @Test
        void incomeTaxIsZeroBecauseRebateExceedsTax() {
            // gravado 1315 - 124.27 = 1190.73; x 12 = 14288.76; IR 104.04 - rebaja 540 < 0 -> 0
            assertThat(r.incomeTaxBase()).isEqualByComparingTo("1190.73");
            assertThat(r.projectedAnnualTaxBase()).isEqualByComparingTo("14288.76");
            assertThat(r.amountOf("INCOME_TAX")).isZero();
        }

        @Test
        void totals() {
            assertThat(r.totalIncome()).isEqualByComparingTo("1424.54");      // 1200 + 75 + 40 + 109.54
            assertThat(r.totalDeductions()).isEqualByComparingTo("124.27");
            assertThat(r.netPay()).isEqualByComparingTo("1300.27");
            // 1424.54 + 159.78 aportes patronales + 204.54 provisiones (109.58 + 40.17 + 54.79)
            assertThat(r.employerCost()).isEqualByComparingTo("1788.86");
        }
    }

    @Nested
    class CaseB_IncomeTaxWithMonthlyBenefits {
        // Sueldo 2500, ingreso 2025-06-01 (sin FR en enero 2026), décimos mensualizados, gastos personales 2000.
        private final PayrollResult r = PayrollCalculator.calculate(
                input("2500", YearMonth.of(2026, 1), LocalDate.of(2025, 6, 1), MONTHLY, MONTHLY, MONTHLY,
                        "0", "0", "0", "2000"), params);

        @Test
        void withholdsProjectedIncomeTax() {
            // (2500 - 236.25) x 12 = 27165 -> IR 1481.75 - 360 = 1121.75 / 12 = 93.48
            assertThat(r.amountOf("IESS_PERSONAL")).isEqualByComparingTo("236.25");
            assertThat(r.amountOf("INCOME_TAX")).isEqualByComparingTo("93.48");
        }

        @Test
        void monthlyBenefitsArePaidInThePayslip() {
            assertThat(r.amountOf("THIRTEENTH_MONTHLY")).isEqualByComparingTo("208.33");
            assertThat(r.amountOf("FOURTEENTH_MONTHLY")).isEqualByComparingTo("40.17");
            assertThat(r.provisionOf(ProvisionType.RESERVE_FUND)).isZero();
            assertThat(r.totalIncome()).isEqualByComparingTo("2748.50");
            assertThat(r.totalDeductions()).isEqualByComparingTo("329.73");
            assertThat(r.netPay()).isEqualByComparingTo("2418.77");
            // 2748.50 + 303.75 aportes + 104.17 vacaciones
            assertThat(r.employerCost()).isEqualByComparingTo("3156.42");
        }
    }

    @Test
    void newHireMidMonthGetsProportionalSalaryAndFourteenth() {
        // Ingreso 16-mar: 30 - 16 + 1 = 15 días -> 600 x 15/30 = 300; décimo cuarto 40.1667 x 0.5 = 20.08
        PayrollResult r = PayrollCalculator.calculate(
                input("600", YearMonth.of(2026, 3), LocalDate.of(2026, 3, 16), MONTHLY, MONTHLY, MONTHLY,
                        "0", "0", "0", "0"), params);
        assertThat(r.workedDays()).isEqualByComparingTo("15");
        assertThat(r.amountOf("SALARY")).isEqualByComparingTo("300.00");
        assertThat(r.amountOf("FOURTEENTH_MONTHLY")).isEqualByComparingTo("20.08");
        assertThat(r.amountOf("IESS_PERSONAL")).isEqualByComparingTo("28.35");
    }

    @Test
    void absencesReduceWorkedDays() {
        // 2 faltas: 28 días -> 900 x 28 / 30 = 840
        PayrollResult r = PayrollCalculator.calculate(
                input("900", YearMonth.of(2026, 2), LocalDate.of(2024, 1, 1), MONTHLY, MONTHLY, MONTHLY,
                        "0", "0", "2", "0"), params);
        assertThat(r.workedDays()).isEqualByComparingTo("28");
        assertThat(r.amountOf("SALARY")).isEqualByComparingTo("840.00");
    }

    @Test
    void reserveFundStartsProportionallyInTheAnniversaryMonth() {
        // Ingreso 2025-03-16 -> derecho desde 2026-03-16: 15 días de 30 -> 1000 x 8.33 % x 0.5 = 41.65
        PayrollResult r = PayrollCalculator.calculate(
                input("1000", YearMonth.of(2026, 3), LocalDate.of(2025, 3, 16), MONTHLY, MONTHLY, MONTHLY,
                        "0", "0", "0", "0"), params);
        assertThat(r.amountOf("RESERVE_FUND_MONTHLY")).isEqualByComparingTo("41.65");
    }

    @Test
    void reserveFundFractionIsZeroDuringFirstYear() {
        assertThat(PayrollCalculator.reserveFundFraction(YearMonth.of(2026, 5), LocalDate.of(2025, 6, 1))).isZero();
        assertThat(PayrollCalculator.reserveFundFraction(YearMonth.of(2026, 7), LocalDate.of(2025, 6, 1))).isEqualByComparingTo("1");
    }

    @Test
    void partTimeUsesProportionalHourlyValueAndFourteenth() {
        // 20 h/semana, sueldo 300: hora = 300 / (240 x 0.5) = 2.50; 4 h al 50 % = 15.00; décimo cuarto 40.1667 x 0.5 = 20.08
        PayrollInput in = new PayrollInput(YearMonth.of(2026, 4), bd("300"), 20, LocalDate.of(2026, 1, 1), null,
                MONTHLY, MONTHLY, MONTHLY, bd("4"), null, null, null, null, null, null, 0, false, null, null);
        PayrollResult r = PayrollCalculator.calculate(in, params);
        assertThat(r.amountOf("OVERTIME_SUPPLEMENTARY")).isEqualByComparingTo("15.00");
        assertThat(r.amountOf("FOURTEENTH_MONTHLY")).isEqualByComparingTo("20.08");
    }

    @Test
    void advancesAndOtherDeductionsReduceNetPay() {
        PayrollInput in = new PayrollInput(YearMonth.of(2026, 1), bd("1000"), 40, LocalDate.of(2025, 9, 1), null,
                ACCUMULATED, ACCUMULATED, MONTHLY, null, null, null, bd("50"), bd("200"), bd("25.50"), null, 0, false, null, null);
        PayrollResult r = PayrollCalculator.calculate(in, params);
        // base 1050 (incluye bono), IESS 99.23 (99.225); neto 1050 - 99.23 - 200 - 25.50 = 725.27
        assertThat(r.iessBase()).isEqualByComparingTo("1050.00");
        assertThat(r.netPay()).isEqualByComparingTo("725.27");
    }

    @Test
    void rejectsOvertimeAboveLegalMaximum() {
        assertThatThrownBy(() -> PayrollCalculator.calculate(
                input("1000", YearMonth.of(2026, 1), LocalDate.of(2025, 1, 1), MONTHLY, MONTHLY, MONTHLY,
                        "49", "0", "0", "0"), params))
                .isInstanceOf(PayrollValidationException.class)
                .hasMessageContaining("48");
    }

    @Test
    void rejectsDeductionsGreaterThanIncome() {
        PayrollInput in = new PayrollInput(YearMonth.of(2026, 1), bd("500"), 40, LocalDate.of(2025, 9, 1), null,
                MONTHLY, MONTHLY, MONTHLY, null, null, null, null, bd("900"), null, null, 0, false, null, null);
        assertThatThrownBy(() -> PayrollCalculator.calculate(in, params)).isInstanceOf(PayrollValidationException.class);
    }

    @Test
    void sameInputAndParametersGiveIdenticalResult() {
        PayrollInput in = input("1234.56", YearMonth.of(2026, 5), LocalDate.of(2020, 2, 29), ACCUMULATED, MONTHLY, ACCUMULATED,
                "7.5", "3", "1", "4500");
        assertThat(PayrollCalculator.calculate(in, params)).isEqualTo(PayrollCalculator.calculate(in, params));
    }

    @Test
    void changingAParameterChangesTheResultWithoutCodeChanges() {
        LegalParameterSet newRates = TestParameters.y2026(Map.of(ParameterCode.IESS_PERSONAL_RATE, bd("0.10")));
        PayrollInput in = input("1000", YearMonth.of(2026, 1), LocalDate.of(2025, 9, 1), MONTHLY, MONTHLY, MONTHLY,
                "0", "0", "0", "0");
        assertThat(PayrollCalculator.calculate(in, newRates).amountOf("IESS_PERSONAL")).isEqualByComparingTo("100.00");
    }

    @Test
    void missingParameterFailsLoudly() {
        LegalParameterSet empty = new LegalParameterSet(LocalDate.of(2030, 1, 31), Map.of(), java.util.List.of(), Map.of());
        assertThatThrownBy(() -> empty.get(ParameterCode.SBU)).isInstanceOf(MissingLegalParameterException.class);
        assertThatThrownBy(empty::taxBrackets).isInstanceOf(MissingLegalParameterException.class);
        assertThatThrownBy(() -> empty.expenseCapBaskets(0)).isInstanceOf(MissingLegalParameterException.class);
    }
}
