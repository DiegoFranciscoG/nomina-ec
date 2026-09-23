package com.dgranda.nominaec.support;

import com.dgranda.nominaec.calculation.LegalParameterSet;
import com.dgranda.nominaec.calculation.ParameterCode;
import com.dgranda.nominaec.calculation.TaxBracket;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.dgranda.nominaec.calculation.ParameterCode.*;

/** 2026 legal values, identical to the V2 migration seed (checked by LegalParameterServiceIT). */
public final class TestParameters {

    private TestParameters() {
    }

    public static LegalParameterSet y2026() {
        return y2026(Map.of());
    }

    public static LegalParameterSet y2026(Map<ParameterCode, BigDecimal> overrides) {
        Map<ParameterCode, BigDecimal> v = new EnumMap<>(ParameterCode.class);
        v.put(SBU, bd("482.00"));
        v.put(IESS_PERSONAL_RATE, bd("0.0945"));
        v.put(IESS_EMPLOYER_RATE, bd("0.1115"));
        v.put(IECE_RATE, bd("0.005"));
        v.put(SECAP_RATE, bd("0.005"));
        v.put(RESERVE_FUND_RATE, bd("0.0833"));
        v.put(OVERTIME_SUPPLEMENTARY_SURCHARGE, bd("0.50"));
        v.put(OVERTIME_EXTRAORDINARY_SURCHARGE, bd("1.00"));
        v.put(OVERTIME_SUPPLEMENTARY_MAX_MONTH, bd("48"));
        v.put(MONTHLY_HOURS_BASE, bd("240"));
        v.put(FULL_TIME_WEEKLY_HOURS, bd("40"));
        v.put(THIRTEENTH_DIVISOR, bd("12"));
        v.put(FOURTEENTH_DIVISOR, bd("12"));
        v.put(VACATION_PROVISION_DIVISOR, bd("24"));
        v.put(VACATION_BASE_DAYS, bd("15"));
        v.put(VACATION_EXTRA_AFTER_YEARS, bd("5"));
        v.put(VACATION_EXTRA_MAX_DAYS, bd("15"));
        v.put(BASIC_FAMILY_BASKET, bd("821.80"));
        v.put(PERSONAL_EXPENSE_REBATE_RATE, bd("0.18"));
        v.put(PERSONAL_EXPENSE_CATASTROPHIC_BASKETS, bd("100"));
        v.put(SEVERANCE_BONUS_RATE, bd("0.25"));
        v.put(SEVERANCE_PRORATE_FRACTION, bd("1"));
        v.put(DISMISSAL_MIN_MONTHS, bd("3"));
        v.put(DISMISSAL_THRESHOLD_YEARS, bd("3"));
        v.put(DISMISSAL_MAX_MONTHS, bd("25"));
        v.put(PROFIT_SHARING_WORKERS, bd("0.10"));
        v.put(PROFIT_SHARING_DEPENDENTS, bd("0.05"));
        v.putAll(overrides);

        List<TaxBracket> brackets = List.of(
                bracket("0", "12208", "0", "0"),
                bracket("12208", "15549", "0", "0.05"),
                bracket("15549", "20188", "167", "0.10"),
                bracket("20188", "26700", "631", "0.12"),
                bracket("26700", "35136", "1412", "0.15"),
                bracket("35136", "46575", "2678", "0.20"),
                bracket("46575", "62005", "4965", "0.25"),
                bracket("62005", "82679", "8823", "0.30"),
                bracket("82679", "109956", "15025", "0.35"),
                bracket("109956", null, "24572", "0.37"));
        Map<Integer, BigDecimal> caps = Map.of(0, bd("7"), 1, bd("9"), 2, bd("11"), 3, bd("14"), 4, bd("17"), 5, bd("20"));
        return new LegalParameterSet(LocalDate.of(2026, 1, 31), v, brackets, caps);
    }

    public static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static TaxBracket bracket(String lower, String upper, String base, String rate) {
        return new TaxBracket(bd(lower), upper == null ? null : bd(upper), bd(base), bd(rate));
    }
}
