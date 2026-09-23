package com.dgranda.nominaec.calculation;

/** Input that violates a labor rule (e.g. overtime above the legal maximum). */
public class PayrollValidationException extends RuntimeException {

    public PayrollValidationException(String message) {
        super(message);
    }
}
