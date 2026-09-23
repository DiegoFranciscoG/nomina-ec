package com.dgranda.nominaec.calculation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Monetary helpers: every persisted amount is rounded to cents with HALF_UP. */
public final class Money {

    public static final MathContext CONTEXT = new MathContext(20, RoundingMode.HALF_UP);
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private Money() {
    }

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal divide(BigDecimal dividend, BigDecimal divisor) {
        return dividend.divide(divisor, CONTEXT);
    }

    public static BigDecimal max(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    public static BigDecimal min(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}
