package br.com.tlf.core.domain.exception;

import java.util.List;

public class TokenNullException extends BusinessException {

    public TokenNullException(String message) {
        super(message, List.of(message), DomainErrorCode.INVALID_TOKEN);
    }
}
