package br.com.tlf.configuration.common.rest.exceptionhandler.error;

import br.com.tlf.configuration.common.rest.exceptionhandler.model.ErrorField;
import br.com.tlf.configuration.common.rest.exceptionhandler.model.Meta;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

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
