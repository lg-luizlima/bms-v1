package br.com.tlf.core.domain.exception;

/**
 * Domain error codes surfaced in {@code ProblemDetailResponse.errorCode}.
 *
 * <p>Each code is the expected HTTP status followed by a discriminator digit. The numeric values
 * are part of the public API contract — never renumber an existing code, only append new ones.
 */
public enum DomainErrorCode {
    BAD_REQUEST(4000),
    INVALID_CPF_PARAMETER(4002),
    MISSING_CUSTOMER_IDENTIFICATION(4003),
    INVALID_TOKEN(4004),
    PRODUCT_NOT_FOUND(4040),
    INVALID_TERM(4221),
    MANDATORY_TERM_NOT_ACCEPTED(4222),
    TERM_NOT_FOUND(4223),
    TERM_OUT_OF_VALIDITY(4224),
    UNEXPECTED_ERROR(5000);

    private final int code;

    DomainErrorCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
