package br.com.tlf.core.domain.exception;

import java.util.List;

public class TermNotFoundException extends BusinessException {

    public TermNotFoundException(String message, List<String> errors) {
        super(message, errors, DomainErrorCode.TERM_NOT_FOUND);
    }

}
