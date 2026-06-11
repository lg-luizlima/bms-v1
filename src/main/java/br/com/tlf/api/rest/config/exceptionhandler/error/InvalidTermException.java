package br.com.tlf.api.rest.config.exceptionhandler.error;

import lombok.Getter;

import java.util.List;

import br.com.tlf.api.rest.config.exceptionhandler.model.ConsentErrorEntry;

@Getter
public class InvalidTermException extends RuntimeException {

    private final List<ConsentErrorEntry> errors;

    public InvalidTermException(String message, List<br.com.tlf.api.rest.config.exceptionhandler.model.ConsentErrorEntry> errors) {
        super(message);
        this.errors = List.of();
    }

}
