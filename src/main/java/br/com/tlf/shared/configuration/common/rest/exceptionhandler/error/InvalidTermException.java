package br.com.tlf.shared.configuration.common.rest.exceptionhandler.error;

import br.com.tlf.shared.configuration.common.rest.exceptionhandler.model.ConsentErrorEntry;
import lombok.Getter;

import java.util.List;

@Getter
public class InvalidTermException extends RuntimeException {

    private final List<ConsentErrorEntry> errors;

    public InvalidTermException(String message, List<br.com.tlf.shared.configuration.common.rest.exceptionhandler.model.ConsentErrorEntry> errors) {
        super(message);
        this.errors = List.of();
    }

}
