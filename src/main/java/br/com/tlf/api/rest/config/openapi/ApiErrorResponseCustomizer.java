package br.com.tlf.api.rest.config.openapi;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.tlf.api.rest.config.exceptionhandler.DomainErrorRegistry;
import br.com.tlf.api.rest.config.exceptionhandler.DomainErrorRegistry.DomainErrorMetadata;
import br.com.tlf.api.rest.config.exceptionhandler.model.ProblemDetailResponse;
import br.com.tlf.core.domain.exception.DomainErrorCode;
import br.com.tlf.shared.util.JsonSerializer;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

/**
 * Builds each error response's description and examples from {@link ApiErrorResponse} (registry-derived
 * examples, grouped by status) and {@link ApiErrorExampleOverride} (a full custom body for one code that
 * the registry can't represent, e.g. a bean-validation `errors[]` entry). Both read from — or are built
 * with the same illustrative constants as — the single source of truth {@code ApiExceptionHandler} uses.
 * Requires the matching {@code @ApiResponse(responseCode = ...)} skeleton to already exist (schema
 * linkage stays declarative).
 */
@Component
@RequiredArgsConstructor
public class ApiErrorResponseCustomizer implements OperationCustomizer {

    public static final String EXAMPLE_TIMESTAMP = "2026-09-01T14:23:05.123Z";
    public static final String EXAMPLE_TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";

    private static final String MEDIA_TYPE = "application/json";

    private final JsonSerializer jsonSerializer;

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        AnnotatedElementUtils.findMergedRepeatableAnnotations(handlerMethod.getMethod(), ApiErrorResponse.class)
                .forEach(annotation -> applyGroup(operation, annotation));
        AnnotatedElementUtils.findMergedRepeatableAnnotations(handlerMethod.getMethod(), ApiErrorExampleOverride.class)
                .forEach(annotation -> applyOverride(operation, annotation));
        return operation;
    }

    private void applyGroup(Operation operation, ApiErrorResponse annotation) {
        DomainErrorCode[] codes = annotation.codes();
        if (codes.length == 0) {
            throw new IllegalStateException("@ApiErrorResponse on \"" + operation.getOperationId()
                    + "\" declares an empty codes() array");
        }

        HttpStatus status = DomainErrorRegistry.metadataFor(codes[0]).status();
        for (DomainErrorCode code : codes) {
            HttpStatus codeStatus = DomainErrorRegistry.metadataFor(code).status();
            if (codeStatus != status) {
                throw new IllegalStateException("@ApiErrorResponse on \"" + operation.getOperationId()
                        + "\" mixes statuses: " + codes[0] + "->" + status + " vs " + code + "->" + codeStatus
                        + " — use one @ApiErrorResponse per HTTP status.");
            }
        }

        ApiResponse apiResponse = requireApiResponse(operation, status,
                "@ApiErrorResponse(codes = ...)");
        apiResponse.setDescription(annotation.description());

        for (DomainErrorCode code : codes) {
            DomainErrorMetadata metadata = DomainErrorRegistry.metadataFor(code);
            ProblemDetailResponse body = ProblemDetailResponse.builder()
                    .errorCode(code.getCode())
                    .message(metadata.title())
                    .details(metadata.exampleDetails())
                    .timestamp(EXAMPLE_TIMESTAMP)
                    .traceId(EXAMPLE_TRACE_ID)
                    .build();
            addExample(operation, apiResponse, code.name(), body);
        }
    }

    private void applyOverride(Operation operation, ApiErrorExampleOverride annotation) {
        DomainErrorCode code = annotation.code();
        HttpStatus status = DomainErrorRegistry.metadataFor(code).status();
        ApiResponse apiResponse = requireApiResponse(operation, status,
                "@ApiErrorExampleOverride(code = " + code + ")");

        ProblemDetailResponse body = DocExampleFactories.instantiate(annotation.factory());
        addExample(operation, apiResponse, code.name(), body);
    }

    private ApiResponse requireApiResponse(Operation operation, HttpStatus status, String source) {
        String responseCode = String.valueOf(status.value());
        ApiResponse apiResponse = operation.getResponses() == null
                ? null : operation.getResponses().get(responseCode);
        if (apiResponse == null) {
            throw new IllegalStateException(source + " on \"" + operation.getOperationId()
                    + "\" resolves to status " + responseCode + " but no @ApiResponse(responseCode = \""
                    + responseCode + "\") exists — add the @ApiResponse skeleton first.");
        }
        return apiResponse;
    }

    private void addExample(Operation operation, ApiResponse apiResponse, String exampleName,
            ProblemDetailResponse body) {
        Content content = apiResponse.getContent();
        if (content == null) {
            content = new Content();
            apiResponse.setContent(content);
        }
        MediaType mediaType = content.get(MEDIA_TYPE);
        if (mediaType == null) {
            mediaType = new MediaType();
            content.addMediaType(MEDIA_TYPE, mediaType);
        }
        if (mediaType.getExamples() != null && mediaType.getExamples().containsKey(exampleName)) {
            throw new IllegalStateException("Duplicate example name \"" + exampleName + "\" on \""
                    + operation.getOperationId() + "\" — remove either the hand-written @ExampleObject "
                    + "or the @ApiErrorResponse/@ApiErrorExampleOverride for this error.");
        }

        JsonNode value = jsonSerializer.toJsonNode(body);
        Example example = new Example().value(value).description(exampleName);
        mediaType.addExamples(exampleName, example);
    }
}
