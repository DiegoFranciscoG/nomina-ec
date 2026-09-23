package com.dgranda.nominaec.service.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Registro Civil algorithm: province 01-24 or 30, third digit below 6, and modulo-10 check digit
 * with coefficients 2,1,2,1,2,1,2,1,2.
 */
public class CedulaValidator implements ConstraintValidator<ValidCedula, String> {

    public static boolean isValid(String value) {
        if (value == null || !value.matches("\\d{10}")) {
            return false;
        }
        int province = Integer.parseInt(value.substring(0, 2));
        if (!((province >= 1 && province <= 24) || province == 30)) {
            return false;
        }
        if (value.charAt(2) - '0' >= 6) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int digit = (value.charAt(i) - '0') * (i % 2 == 0 ? 2 : 1);
            sum += digit > 9 ? digit - 9 : digit;
        }
        int check = (10 - sum % 10) % 10;
        return check == value.charAt(9) - '0';
    }

    /** "1710034065" -> "17******65". */
    public static String mask(String value) {
        if (value == null || value.length() < 4) {
            return "****";
        }
        return value.substring(0, 2) + "*".repeat(value.length() - 4) + value.substring(value.length() - 2);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || isValid(value);
    }
}
