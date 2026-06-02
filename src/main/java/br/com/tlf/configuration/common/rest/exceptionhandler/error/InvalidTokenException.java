package br.com.tlf.configuration.common.rest.exceptionhandler.error;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message, Object... args) {
        super(String.format(message, args));
    }

}