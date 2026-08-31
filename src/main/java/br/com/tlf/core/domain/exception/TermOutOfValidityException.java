package br.com.tlf.core.domain.exception;

import java.util.List;

public class TermOutOfValidityException extends BusinessException {

    public TermOutOfValidityException(String message, List<String> errors) {
        super(message, errors, DomainErrorCode.TERM_OUT_OF_VALIDITY);
    }

}
