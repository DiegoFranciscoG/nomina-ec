package com.dgranda.nominaec.exception;

/** The resource state does not allow the operation, e.g. recalculating a closed period (HTTP 409). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
