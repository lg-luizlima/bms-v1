package br.com.tlf.api.rest.config.exceptionhandler.error;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message, Object... args) {
        super(String.format(message, args));
    }

}