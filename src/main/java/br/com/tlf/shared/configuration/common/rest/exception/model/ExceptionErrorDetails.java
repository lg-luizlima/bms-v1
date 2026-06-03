package br.com.tlf.shared.configuration.common.rest.exception.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

import br.com.tlf.shared.configuration.common.rest.exceptionhandler.model.ErrorField;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class ExceptionErrorDetails implements Serializable {

    @Serial
    private static final long serialVersionUID = -6455965296955705150L;

    private String status;
    private String message;
    private List<ErrorField> errors;
    private Meta meta;
}
