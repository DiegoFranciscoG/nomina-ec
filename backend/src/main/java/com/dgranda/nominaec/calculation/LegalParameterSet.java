package com.dgranda.nominaec.calculation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Immutable snapshot of every legal value in force on {@code effectiveDate}.
 * The calculators receive this object instead of querying the database, which keeps them pure
 * and makes each payslip reproducible from its stored snapshot.
 */
public final class LegalParameterSet {

    private final LocalDate effectiveDate;
    private final Map<ParameterCode, BigDecimal> values;
    private final List<TaxBracket> taxBrackets;
    private final TreeMap<Integer, BigDecimal> expenseCapBaskets;

    public LegalParameterSet(LocalDate effectiveDate,
                             Map<ParameterCode, BigDecimal> values,
                             List<TaxBracket> taxBrackets,
                             Map<Integer, BigDecimal> expenseCapBaskets) {
        this.effectiveDate = effectiveDate;
        this.values = new EnumMap<>(ParameterCode.class);
        this.values.putAll(values);
        this.taxBrackets = taxBrackets.stream().sorted(Comparator.comparing(TaxBracket::lowerBound)).toList();
        this.expenseCapBaskets = new TreeMap<>(expenseCapBaskets);
    }

    public LocalDate effectiveDate() {
        return effectiveDate;
    }

    public BigDecimal get(ParameterCode code) {
        BigDecimal value = values.get(code);
        if (value == null) {
            throw new MissingLegalParameterException(code.name(), effectiveDate);
        }
        return value;
    }

    public int getInt(ParameterCode code) {
        return get(code).intValueExact();
    }

    public List<TaxBracket> taxBrackets() {
        if (taxBrackets.isEmpty()) {
            throw new MissingLegalParameterException("INCOME_TAX_BRACKETS", effectiveDate);
        }
        return taxBrackets;
    }

    /** Number of basic family baskets allowed for the given dependents (the highest row covers "or more"). */
    public BigDecimal expenseCapBaskets(int familyDependents) {
        if (expenseCapBaskets.isEmpty()) {
            throw new MissingLegalParameterException("PERSONAL_EXPENSE_CAPS", effectiveDate);
        }
        int key = Math.min(familyDependents, expenseCapBaskets.lastKey());
        return expenseCapBaskets.get(key);
    }

    /** Serializable view stored in {@code payslips.parameters_snapshot}. */
    public Map<String, Object> toSnapshot() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("effectiveDate", effectiveDate.toString());
        Map<String, BigDecimal> params = new TreeMap<>();
        values.forEach((k, v) -> params.put(k.name(), v.stripTrailingZeros()));
        snapshot.put("parameters", params);
        snapshot.put("taxBrackets", taxBrackets);
        snapshot.put("personalExpenseCapBaskets", expenseCapBaskets);
        return snapshot;
    }
}
