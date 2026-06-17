package br.com.tlf.core.domain.exception;

import java.util.List;

public class MandatoryTermNotAcceptedException extends BusinessException {

    public MandatoryTermNotAcceptedException(String message, List<String> errors) {
        super(message, errors, DomainErrorCode.MANDATORY_TERM_NOT_ACCEPTED);
    }
    
}
