package br.com.tlf.core.domain.exception;

import java.util.List;

public class InvalidTermException extends BusinessException {

    public InvalidTermException(String message, List<String> errors) {
        super(message, errors, DomainErrorCode.INVALID_TERM);
    }

}
