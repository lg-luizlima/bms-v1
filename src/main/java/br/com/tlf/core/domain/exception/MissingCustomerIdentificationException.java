package br.com.tlf.core.domain.exception;

import java.util.List;

public class MissingCustomerIdentificationException extends BusinessException {

    public MissingCustomerIdentificationException(String message, List<String> errors) {
        super(message, errors, DomainErrorCode.MISSING_CUSTOMER_IDENTIFICATION);
    }
}
