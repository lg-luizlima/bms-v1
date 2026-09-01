package br.com.tlf.core.domain.exception;

import java.util.List;

public class InvalidTokenException extends BusinessException {

    public InvalidTokenException(String message) {
        super(message, List.of(message), DomainErrorCode.INVALID_TOKEN);
    }
}
