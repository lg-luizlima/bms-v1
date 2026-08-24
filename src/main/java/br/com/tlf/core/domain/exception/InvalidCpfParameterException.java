package br.com.tlf.core.domain.exception;

import java.util.List;

public class InvalidCpfParameterException extends BusinessException {

    public InvalidCpfParameterException(String message, List<String> errors) {
        super(message, errors, DomainErrorCode.INVALID_CPF_PARAMETER);
    }

}
