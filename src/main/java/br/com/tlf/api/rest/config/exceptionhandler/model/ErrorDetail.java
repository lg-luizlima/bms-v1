package br.com.tlf.api.rest.config.exceptionhandler.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "One individual error, either tied to a request field or to a business rule")
public class ErrorDetail {

    @Schema(description = "Optional numeric sub-code for this specific error, when applicable")
    private Integer code;

    @Schema(description = "Name/path of the request field this error refers to, when the error is field-level",
            example = "signature.deviceId")
    private String field;

    @Schema(description = "Human-readable description of this error", example = "must not be blank",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;
}
