package br.com.tlf.core.domain.customer;

import java.util.List;

import br.com.tlf.core.domain.exception.InvalidCpfParameterException;


public record Cpf(String value) {

    private static final String DIGITS_ONLY = "\\d{11}";
    private static final int CHECK_DIGIT_ONE_INDEX = 9;
    private static final int CHECK_DIGIT_TWO_INDEX = 10;

    public static Cpf of(String value) {
        if (!isValid(value)) {
            throw new InvalidCpfParameterException("Invalid CPF parameter",
                    List.of("x-customer-id must be a valid CPF with 11 numeric digits"));
        }
        return new Cpf(value);
    }

    public static boolean isValid(String value) {
        if (value == null || !value.matches(DIGITS_ONLY)) {
            return false;
        }
        if (value.chars().distinct().count() == 1) {
            return false;
        }
        return checkDigit(value, CHECK_DIGIT_ONE_INDEX, 10) == digitAt(value, CHECK_DIGIT_ONE_INDEX)
                && checkDigit(value, CHECK_DIGIT_TWO_INDEX, 11) == digitAt(value, CHECK_DIGIT_TWO_INDEX);
    }

    private static int checkDigit(String cpf, int length, int weightStart) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += digitAt(cpf, i) * (weightStart - i);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static int digitAt(String cpf, int index) {
        return Character.getNumericValue(cpf.charAt(index));
    }
}
