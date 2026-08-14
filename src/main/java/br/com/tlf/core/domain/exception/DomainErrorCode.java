package br.com.tlf.core.domain.exception;

/**
 * Enum for domain error codes, used in custom exceptions.
 * Each code is a unique integer (expected HTTP status + additional digits) representing a specific type of business error.
 */
public enum DomainErrorCode {
    BAD_REQUEST(4000),
    MISSING_AUDIT_DATA(4001),
    PRODUCT_NOT_FOUND(4040),
    INVALID_TERM(4221),
    MANDATORY_TERM_NOT_ACCEPTED(4222),
    UNEXPECTED_ERROR(5000);

    DomainErrorCode(int code) {
        this.code = code;
    }

    private final int code;

    public int getCode() {
        return code;
    }

}
