package br.com.tlf.shared.configuration.common.rest.exceptionhandler.error;

public class MateraIntegrationUnauthorizedException extends RuntimeException {

    public MateraIntegrationUnauthorizedException(String message, Object... args) {
        super(String.format(message, args));
    }

}
