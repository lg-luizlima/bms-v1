package br.com.tlf.api.rest.config.exceptionhandler.error;

import lombok.Getter;

import java.util.List;

import br.com.tlf.api.rest.config.exceptionhandler.model.ConsentErrorEntry;

@Getter
public class MissingAuditDataException extends RuntimeException {

    private final List<ConsentErrorEntry> errors;

    public MissingAuditDataException(List<ConsentErrorEntry> errors) {
        super("Missing required audit data in signature");
        this.errors = errors;
    }
}
