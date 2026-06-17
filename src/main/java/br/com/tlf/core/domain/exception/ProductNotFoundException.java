package br.com.tlf.core.domain.exception;

import java.util.List;

public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException(String message, List<String> errors) {
        super(message, errors, DomainErrorCode.PRODUCT_NOT_FOUND);
    }

}
