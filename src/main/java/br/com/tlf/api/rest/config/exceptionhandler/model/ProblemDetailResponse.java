package br.com.tlf.api.rest.config.exceptionhandler.model;

import java.util.List;

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
@Schema(description = "Standard error envelope returned by every non-2xx response")
public class ProblemDetailResponse {

    @Schema(description = "Numeric domain error code (see DomainErrorCode) — the digit(s) before the last one "
            + "match the expected HTTP status", example = "4222", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer errorCode;

    @Schema(description = "Short title of the error category", example = "Business Validation Error",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "Human-readable detail explaining what went wrong",
            example = "Mandatory term was not accepted.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String details;

    @Schema(description = "Instant the error was produced, in ISO-8601 UTC", example = "2026-09-01T14:23:05.123Z",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String timestamp;

    @Schema(description = "OpenTelemetry trace id of the request that failed, for correlation with logs/traces",
            example = "4bf92f3577b34da6a3ce929d0e0e4736", requiredMode = Schema.RequiredMode.REQUIRED)
    private String traceId;

    @Schema(description = "Field-level or business-rule-level error details, when applicable")
    private List<ErrorDetail> errors;
}
