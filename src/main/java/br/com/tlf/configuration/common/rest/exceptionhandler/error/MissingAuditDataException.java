package br.com.tlf.configuration.common.rest.exceptionhandler.error;

import br.com.tlf.configuration.common.rest.exceptionhandler.model.ConsentErrorEntry;
import lombok.Getter;

import java.util.List;

@Getter
public class MissingAuditDataException extends RuntimeException {

    private final List<ConsentErrorEntry> errors;

    public MissingAuditDataException(List<ConsentErrorEntry> errors) {
        super("Missing required audit data in signature");
        this.errors = errors;
    }
}
