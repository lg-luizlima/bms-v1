package br.com.tlf.shared.configuration.common.rest.dto.v1.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class ExceptionErrorDetailsDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -2800814814664968657L;

    private String status;
    private String message;
    private List<ErrorDTO> errors;
    private MetaDTO meta;
    private String data;
    private String title;
    private String description;
    private String code;
    private String detail;
    private Integer statusCode;

}
