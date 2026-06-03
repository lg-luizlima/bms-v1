package br.com.tlf.configuration.common.rest.exceptionhandler.error;

import br.com.tlf.configuration.common.rest.exceptionhandler.model.ConsentErrorEntry;
import lombok.Getter;

import java.util.List;

@Getter
public class InvalidTermException extends RuntimeException {

    private final List<ConsentErrorEntry> errors;

    public InvalidTermException(String message) {
        super(message);
        this.errors = List.of();
    }

    public InvalidTermException(String message, List<ConsentErrorEntry> errors) {
        super(message);
        this.errors = errors;
    }
}
