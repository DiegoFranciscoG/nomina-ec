package com.dgranda.nominaec.calculation;

import com.dgranda.nominaec.entity.PaymentMode;
import com.dgranda.nominaec.entity.Region;
import com.dgranda.nominaec.entity.SettlementReason;
import com.dgranda.nominaec.support.TestParameters;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static com.dgranda.nominaec.support.TestParameters.bd;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Hand-verified final settlements (30/360 commercial calendar). */
class SettlementCalculatorTest {

    private final LegalParameterSet params = TestParameters.y2026();

    private static BigDecimal item(SettlementCalculator.Result r, String code) {
        return r.items().stream().filter(i -> i.code().equals(code)).map(SettlementCalculator.Item::amount)
                .findFirst().orElse(BigDecimal.ZERO);
    }

    @Test
    void unjustifiedDismissalAfterThreeYears() {
        // Ingreso 2023-03-01, salida 2026-06-15: 1080 + 90 + 14 + 1 = 1185 días = 3.2917 años
        var in = new SettlementCalculator.Input(bd("1000"), 40, LocalDate.of(2023, 3, 1), LocalDate.of(2026, 6, 15),
                SettlementReason.UNJUSTIFIED_DISMISSAL, Region.SIERRA_AMAZONIA, PaymentMode.ACCUMULATED, PaymentMode.ACCUMULATED,
                bd("450.00"), bd("15"), BigDecimal.ZERO);
        var r = SettlementCalculator.calculate(in, params);

        assertThat(r.serviceDays()).isEqualTo(1185);
        assertThat(item(r, "PENDING_SALARY")).isEqualByComparingTo("500.00");          // 1000 x 15/30
        assertThat(item(r, "THIRTEENTH")).isEqualByComparingTo("491.67");              // 450 + 500/12
        assertThat(item(r, "FOURTEENTH")).isEqualByComparingTo("421.75");              // 482 x 315/360 (desde 2025-08-01)
        assertThat(item(r, "VACATION_PROPORTIONAL")).isEqualByComparingTo("145.83");   // 1000/30 x 15 x 105/360
        assertThat(item(r, "SEVERANCE_BONUS")).isEqualByComparingTo("822.92");         // 1000 x 25 % x 3.2917
        assertThat(item(r, "DISMISSAL_INDEMNITY")).isEqualByComparingTo("4000.00");    // fracción cuenta como año: 4 x 1000
        assertThat(r.totalIncome()).isEqualByComparingTo("6382.17");
        assertThat(r.totalDeductions()).isEqualByComparingTo("47.25");                 // 500 x 9.45 %
        assertThat(r.netTotal()).isEqualByComparingTo("6334.92");
    }

    @Test
    void resignationWithMonthlyBenefits() {
        // 2025-01-01 -> 2026-01-31: 390 días; décimos mensualizados: solo la parte del sueldo pendiente
        var in = new SettlementCalculator.Input(bd("800"), 40, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 31),
                SettlementReason.RESIGNATION, Region.COSTA_GALAPAGOS, PaymentMode.MONTHLY, PaymentMode.MONTHLY,
                null, bd("30"), null);
        var r = SettlementCalculator.calculate(in, params);

        assertThat(r.serviceDays()).isEqualTo(390);
        assertThat(item(r, "PENDING_SALARY")).isEqualByComparingTo("800.00");
        assertThat(item(r, "THIRTEENTH")).isEqualByComparingTo("66.67");
        assertThat(item(r, "FOURTEENTH")).isEqualByComparingTo("40.17");
        assertThat(item(r, "VACATION_PROPORTIONAL")).isEqualByComparingTo("33.33");    // 800/30 x 15 x 30/360
        assertThat(item(r, "SEVERANCE_BONUS")).isEqualByComparingTo("216.67");         // 800 x 25 % x 1.0833
        assertThat(item(r, "DISMISSAL_INDEMNITY")).isZero();
        assertThat(r.netTotal()).isEqualByComparingTo("1081.24");                      // 1156.84 - 75.60
    }

    @Test
    void justifiedDismissalHasNoSeveranceBonus() {
        var in = new SettlementCalculator.Input(bd("800"), 40, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 31),
                SettlementReason.JUSTIFIED_DISMISSAL, Region.COSTA_GALAPAGOS, PaymentMode.MONTHLY, PaymentMode.MONTHLY,
                null, BigDecimal.ZERO, bd("5"));
        var r = SettlementCalculator.calculate(in, params);
        assertThat(item(r, "SEVERANCE_BONUS")).isZero();
        assertThat(item(r, "VACATION_UNUSED")).isEqualByComparingTo("133.33");        // 800/30 x 5
    }

    @Test
    void dismissalWithinThreeYearsPaysThreeSalariesAndCapsAtTwentyFive() {
        var shortService = new SettlementCalculator.Input(bd("700"), 40, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 31),
                SettlementReason.UNJUSTIFIED_DISMISSAL, Region.COSTA_GALAPAGOS, PaymentMode.MONTHLY, PaymentMode.MONTHLY,
                null, null, null);
        assertThat(item(SettlementCalculator.calculate(shortService, params), "DISMISSAL_INDEMNITY")).isEqualByComparingTo("2100.00");

        var longService = new SettlementCalculator.Input(bd("700"), 40, LocalDate.of(1990, 1, 1), LocalDate.of(2026, 1, 31),
                SettlementReason.UNJUSTIFIED_DISMISSAL, Region.COSTA_GALAPAGOS, PaymentMode.MONTHLY, PaymentMode.MONTHLY,
                null, null, null);
        assertThat(item(SettlementCalculator.calculate(longService, params), "DISMISSAL_INDEMNITY")).isEqualByComparingTo("17500.00");
    }

    @Test
    void severanceCanCountOnlyCompleteYearsViaParameter() {
        LegalParameterSet onlyComplete = TestParameters.y2026(Map.of(ParameterCode.SEVERANCE_PRORATE_FRACTION, BigDecimal.ZERO));
        var in = new SettlementCalculator.Input(bd("800"), 40, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 31),
                SettlementReason.MUTUAL_AGREEMENT, Region.COSTA_GALAPAGOS, PaymentMode.MONTHLY, PaymentMode.MONTHLY,
                null, null, null);
        assertThat(item(SettlementCalculator.calculate(in, onlyComplete), "SEVERANCE_BONUS")).isEqualByComparingTo("200.00");
    }

    @Test
    void fourteenthCycleDependsOnRegion() {
        assertThat(SettlementCalculator.fourteenthCycleStart(Region.SIERRA_AMAZONIA, LocalDate.of(2026, 7, 31)))
                .isEqualTo(LocalDate.of(2025, 8, 1));
        assertThat(SettlementCalculator.fourteenthCycleStart(Region.SIERRA_AMAZONIA, LocalDate.of(2026, 8, 1)))
                .isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(SettlementCalculator.fourteenthCycleStart(Region.COSTA_GALAPAGOS, LocalDate.of(2026, 2, 28)))
                .isEqualTo(LocalDate.of(2025, 3, 1));
        assertThat(SettlementCalculator.fourteenthCycleStart(Region.COSTA_GALAPAGOS, LocalDate.of(2026, 3, 1)))
                .isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    void vacationDaysGrowAfterFifthYearUpToCap() {
        assertThat(SettlementCalculator.vacationDaysForYear(1, params)).isEqualByComparingTo("15");
        assertThat(SettlementCalculator.vacationDaysForYear(5, params)).isEqualByComparingTo("15");
        assertThat(SettlementCalculator.vacationDaysForYear(7, params)).isEqualByComparingTo("17");
        assertThat(SettlementCalculator.vacationDaysForYear(40, params)).isEqualByComparingTo("30");
    }

    @Test
    void rejectsTerminationBeforeStart() {
        var in = new SettlementCalculator.Input(bd("800"), 40, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 31),
                SettlementReason.RESIGNATION, Region.COSTA_GALAPAGOS, PaymentMode.MONTHLY, PaymentMode.MONTHLY,
                null, null, null);
        assertThatThrownBy(() -> SettlementCalculator.calculate(in, params)).isInstanceOf(PayrollValidationException.class);
    }
}
