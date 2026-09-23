package com.dgranda.nominaec.calculation;

import java.time.LocalDate;

/** Raised when no version of a legal parameter is in force for the requested date. */
public class MissingLegalParameterException extends RuntimeException {

    public MissingLegalParameterException(String code, LocalDate date) {
        super("No hay un valor vigente del parámetro legal " + code + " para la fecha " + date);
    }
}
