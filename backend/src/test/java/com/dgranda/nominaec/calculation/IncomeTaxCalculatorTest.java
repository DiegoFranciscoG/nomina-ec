package com.dgranda.nominaec.calculation;

import com.dgranda.nominaec.support.TestParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static com.dgranda.nominaec.support.TestParameters.bd;
import static org.assertj.core.api.Assertions.assertThat;

/** Expected values computed by hand with the 2026 SRI table (NAC-DGERCGC25-00000043). */
class IncomeTaxCalculatorTest {

    private final LegalParameterSet params = TestParameters.y2026();

    @ParameterizedTest(name = "base {0} -> impuesto {1}")
    @CsvSource({
            "0,          0.00",
            "12208,      0.00",     // fracción básica desgravada
            "14288.76, 104.04",    // (14288.76 - 12208) x 5 % = 104.038
            "20000,    612.10",    // 167 + (20000 - 15549) x 10 %
            "27165,   1481.75",    // 1412 + (27165 - 26700) x 15 %
            "120000, 28288.28"     // 24572 + (120000 - 109956) x 37 %
    })
    void annualTaxFollowsTheProgressiveTable(String base, String expected) {
        assertThat(Money.round(IncomeTaxCalculator.annualTax(bd(base), params))).isEqualByComparingTo(expected);
    }

    @Test
    void rebateIsEighteenPercentOfTheLowerBetweenExpensesAndCap() {
        // 0 cargas -> 7 canastas x 821.80 = 5752.60; gastos 3000 < tope -> 18 % x 3000 = 540
        assertThat(IncomeTaxCalculator.personalExpenseRebate(bd("3000"), 0, false, params)).isEqualByComparingTo("540");
        // gastos 9000 > tope 5752.60 -> 18 % x 5752.60 = 1035.468
        assertThat(IncomeTaxCalculator.personalExpenseRebate(bd("9000"), 0, false, params)).isEqualByComparingTo("1035.468");
    }

    @Test
    void capGrowsWithDependentsAndTopsAtFiveOrMore() {
        assertThat(IncomeTaxCalculator.personalExpenseCap(2, false, params)).isEqualByComparingTo("9039.80");   // 11 canastas
        assertThat(IncomeTaxCalculator.personalExpenseCap(5, false, params)).isEqualByComparingTo("16436.00");  // 20 canastas
        assertThat(IncomeTaxCalculator.personalExpenseCap(8, false, params)).isEqualByComparingTo("16436.00");  // "5 o más"
    }

    @Test
    void catastrophicConditionUsesOneHundredBaskets() {
        assertThat(IncomeTaxCalculator.personalExpenseCap(0, true, params)).isEqualByComparingTo("82180.00");
        assertThat(IncomeTaxCalculator.personalExpenseRebate(bd("10000"), 0, true, params)).isEqualByComparingTo("1800");
    }

    @Test
    void taxDueNeverGoesNegative() {
        assertThat(IncomeTaxCalculator.annualTaxDue(bd("14288.76"), bd("3000"), 0, false, params)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void monthlyWithholdingSpreadsAnnualTaxOverRemainingMonths() {
        // Enero: proyección 2263.75 x 12 = 27165; impuesto 1481.75 - rebaja 360 = 1121.75; / 12 = 93.479 -> 93.48
        var january = IncomeTaxCalculator.monthlyWithholding(bd("2263.75"), BigDecimal.ZERO, BigDecimal.ZERO, 1,
                bd("2000"), 0, false, params);
        assertThat(january.projectedAnnualBase()).isEqualByComparingTo("27165.00");
        assertThat(january.annualTaxDue()).isEqualByComparingTo("1121.75");
        assertThat(january.monthlyWithholding()).isEqualByComparingTo("93.48");

        // Febrero: (1121.75 - 93.48) / 11 = 93.479 -> 93.48 (se reliquida con lo ya retenido)
        var february = IncomeTaxCalculator.monthlyWithholding(bd("2263.75"), bd("2263.75"), bd("93.48"), 2,
                bd("2000"), 0, false, params);
        assertThat(february.monthlyWithholding()).isEqualByComparingTo("93.48");
    }

    @Test
    void raiseInJulyIsReliquidatedOverRemainingMonths() {
        // Ene-jun: 6 x 2263.75 = 13582.50 gravado, 6 x 93.48 = 560.88 retenido.
        // Julio: nuevo gravado 3000 -> proyección 13582.50 + 3000 x 6 = 31582.50
        // impuesto 1412 + (31582.50 - 26700) x 15 % = 2144.375; - 360 = 1784.375; pendiente 1223.495 / 6 = 203.9158 -> 203.92
        var july = IncomeTaxCalculator.monthlyWithholding(bd("3000"), bd("13582.50"), bd("560.88"), 7,
                bd("2000"), 0, false, params);
        assertThat(july.projectedAnnualBase()).isEqualByComparingTo("31582.50");
        assertThat(july.monthlyWithholding()).isEqualByComparingTo("203.92");
    }
}
