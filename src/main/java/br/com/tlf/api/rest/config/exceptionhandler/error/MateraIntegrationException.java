package br.com.tlf.api.rest.config.exceptionhandler.error;

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
