package com.dgranda.nominaec.calculation;

import com.dgranda.nominaec.entity.PaymentMode;
import com.dgranda.nominaec.support.TestParameters;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static com.dgranda.nominaec.support.TestParameters.bd;
import static org.assertj.core.api.Assertions.assertThat;

class HiringCostSimulatorTest {

    private final LegalParameterSet params = TestParameters.y2026();

    @Test
    void costOfHiringAtMinimumWage() {
        var r = HiringCostSimulator.simulate(new HiringCostSimulator.Input(bd("482"), 40, YearMonth.of(2026, 1),
                PaymentMode.MONTHLY, PaymentMode.MONTHLY), params);

        var first = r.firstYear();
        assertThat(first.employerIess()).isEqualByComparingTo("53.74");   // 482 x 11.15 % = 53.743
        assertThat(first.iece()).isEqualByComparingTo("2.41");
        assertThat(first.secap()).isEqualByComparingTo("2.41");
        assertThat(first.thirteenth()).isEqualByComparingTo("40.17");
        assertThat(first.fourteenth()).isEqualByComparingTo("40.17");
        assertThat(first.reserveFund()).isZero();
        assertThat(first.vacation()).isEqualByComparingTo("20.08");
        // 482 + 40.17 + 40.17 + 53.74 + 2.41 + 2.41 + 20.08
        assertThat(first.monthlyCost()).isEqualByComparingTo("640.98");
        assertThat(first.annualCost()).isEqualByComparingTo("7691.76");
        assertThat(first.costOverSalary()).isEqualByComparingTo("1.3298");
        assertThat(first.employeeNetMonthly()).isEqualByComparingTo("516.79");  // 562.34 - 45.55

        // Desde el segundo año se suma el fondo de reserva: 482 x 8.33 % = 40.15
        assertThat(r.fromSecondYear().reserveFund()).isEqualByComparingTo("40.15");
        assertThat(r.fromSecondYear().monthlyCost()).isEqualByComparingTo("681.13");
        assertThat(r.belowMinimumWage()).isFalse();
    }

    @Test
    void flagsSalaryBelowMinimumWage() {
        var r = HiringCostSimulator.simulate(new HiringCostSimulator.Input(bd("400"), 40, YearMonth.of(2026, 1),
                PaymentMode.ACCUMULATED, PaymentMode.ACCUMULATED), params);
        assertThat(r.belowMinimumWage()).isTrue();
        assertThat(r.sbu()).isEqualByComparingTo("482");
    }
}
