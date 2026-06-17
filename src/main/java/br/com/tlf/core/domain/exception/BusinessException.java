package br.com.tlf.core.domain.exception;

import java.util.List;

public class BusinessException extends RuntimeException {

    private final List<String> errors;
    private final DomainErrorCode errorCode;

    public BusinessException(String message, List<String> errors, DomainErrorCode errorCode) {
        super(message);
        this.errors = List.copyOf(errors);
        this.errorCode = errorCode;
    }

    public List<String> getErrors() {
        return errors;
    }

    public DomainErrorCode getErrorCode() {
        return errorCode;
    }
}
