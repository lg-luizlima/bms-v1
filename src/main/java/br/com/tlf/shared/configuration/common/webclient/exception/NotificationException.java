package br.com.tlf.shared.configuration.common.webclient.exception;

import java.util.List;

import br.com.tlf.api.rest.config.exceptionhandler.model.ErrorField;
import br.com.tlf.api.rest.config.exceptionhandler.model.Meta;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationException extends RuntimeException {

    private final String status;
    private final String message;
    private final List<ErrorField> errors;
    private final Meta meta;
    private final Integer value;
    private final String data;
    private final String title;
    private final String description;
    private final String code;
}
