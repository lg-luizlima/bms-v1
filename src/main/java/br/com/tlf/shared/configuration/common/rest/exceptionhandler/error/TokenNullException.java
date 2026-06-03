package br.com.tlf.shared.configuration.common.rest.exceptionhandler.error;

public class TokenNullException extends RuntimeException {
    public TokenNullException(String message, Object... args) {
        super(String.format(message, args));
    }
}