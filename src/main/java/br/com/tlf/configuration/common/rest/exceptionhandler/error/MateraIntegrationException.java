package br.com.tlf.configuration.common.rest.exceptionhandler.error;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MateraIntegrationException extends RuntimeException {
    
    public MateraIntegrationException() {
    }

    public MateraIntegrationException(String message, Object... args) {
        super(String.format(message, args));
    }

}
