package br.com.tlf.api.rest.config.exceptionhandler.error;

public class TokenNullException extends RuntimeException {
    public TokenNullException(String message, Object... args) {
        super(String.format(message, args));
    }
}