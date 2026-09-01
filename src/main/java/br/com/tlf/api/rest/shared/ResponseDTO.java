package br.com.tlf.api.rest.shared;

import java.io.Serial;
import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard success envelope wrapping every 2xx response payload")
public class ResponseDTO<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = -5087991643187068240L;

    @Schema(description = "Response payload", requiredMode = Schema.RequiredMode.REQUIRED)
    private T data;

    @Schema(description = "Outcome status of the request", requiredMode = Schema.RequiredMode.REQUIRED)
    private ResponseStatus status;

    @Schema(description = "Human-readable summary of the outcome", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    public static <T> ResponseDTO<T> ok(T data, String message) {
        return ResponseDTO.<T>builder().data(data).status(ResponseStatus.OK).message(message).build();
    }

    public static <T> ResponseDTO<T> success(T data, String message) {
        return ResponseDTO.<T>builder().data(data).status(ResponseStatus.SUCCESS).message(message).build();
    }
}
