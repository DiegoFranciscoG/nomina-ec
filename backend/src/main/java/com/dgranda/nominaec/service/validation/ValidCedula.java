package com.dgranda.nominaec.service.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Ecuadorian national id (cédula): 10 digits, valid province and modulo-10 check digit. */
@Documented
@Constraint(validatedBy = CedulaValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCedula {

    String message() default "cédula ecuatoriana inválida";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
