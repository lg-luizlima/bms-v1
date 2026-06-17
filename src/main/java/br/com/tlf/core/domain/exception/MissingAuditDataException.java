package br.com.tlf.core.domain.exception;

import java.util.List;

public class MissingAuditDataException extends BusinessException {

    public MissingAuditDataException(String message, List<String> errors) {
        super(message, errors, DomainErrorCode.MISSING_AUDIT_DATA);
    }

}
