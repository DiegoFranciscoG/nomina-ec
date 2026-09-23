package com.dgranda.nominaec.calculation;

import java.math.BigDecimal;

/** One row of the SRI progressive income tax table. {@code upperBound == null} means "and above". */
public record TaxBracket(BigDecimal lowerBound, BigDecimal upperBound, BigDecimal baseTax, BigDecimal marginalRate) {

    public boolean contains(BigDecimal amount) {
        return amount.compareTo(lowerBound) >= 0 && (upperBound == null || amount.compareTo(upperBound) <= 0);
    }
}
