package br.com.tlf.core.domain.exception;

import java.util.List;

public class BusinessException extends RuntimeException {

    private final List<String> errors;

    public BusinessException(String message, List<String> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
    
}
