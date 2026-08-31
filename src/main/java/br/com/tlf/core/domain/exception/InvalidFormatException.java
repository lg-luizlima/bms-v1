package br.com.tlf.core.domain.exception;

import java.util.List;

public class InvalidFormatException extends BusinessException {

    public InvalidFormatException(String message, List<String> errors) {
        super(message, errors, DomainErrorCode.INVALID_TERM);
    }

}
