package com.dgranda.nominaec.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource, Object id) {
        super(resource + " no encontrado: " + id);
    }
}
