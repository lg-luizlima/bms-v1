package br.com.tlf.api.rest.config.exceptionhandler.error;

public class MateraIntegrationUnauthorizedException extends RuntimeException {

    public MateraIntegrationUnauthorizedException(String message, Object... args) {
        super(String.format(message, args));
    }

}
