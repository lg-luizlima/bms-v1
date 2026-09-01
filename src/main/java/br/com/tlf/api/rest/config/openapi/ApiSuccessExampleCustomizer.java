package br.com.tlf.api.rest.config.openapi;

import java.util.List;
import java.util.Set;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.tlf.api.rest.shared.ResponseDTO;
import br.com.tlf.shared.util.JsonSerializer;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

/**
 * Builds the full {@code ResponseDTO} envelope example (data + status + message) for each named
 * {@link ApiSuccessExample} on an endpoint's 2xx response, instead of a hand-typed example. See
 * {@link ApiSuccessExample} for why this can never drift from what the endpoint actually returns.
 */
@Component
@RequiredArgsConstructor
public class ApiSuccessExampleCustomizer implements OperationCustomizer {

    private static final String FALLBACK_MEDIA_TYPE = "application/json";

    private final JsonSerializer jsonSerializer;

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        Set<ApiSuccessExample> annotations =
                AnnotatedElementUtils.findMergedRepeatableAnnotations(handlerMethod.getMethod(), ApiSuccessExample.class);
        if (annotations.isEmpty()) {
            return operation;
        }

        String responseCode = find2xxKey(operation);
        ApiResponse apiResponse = operation.getResponses().get(responseCode);

        Content content = apiResponse.getContent();
        if (content == null || content.isEmpty()) {
            content = new Content();
            content.addMediaType(FALLBACK_MEDIA_TYPE, new MediaType());
            apiResponse.setContent(content);
        }

        Content responseContent = content;
        annotations.forEach(annotation -> addExample(operation, responseContent, annotation));
        return operation;
    }

    private void addExample(Operation operation, Content content, ApiSuccessExample annotation) {
        for (MediaType mediaType : content.values()) {
            if (mediaType.getExamples() != null && mediaType.getExamples().containsKey(annotation.name())) {
                throw new IllegalStateException("Duplicate @ApiSuccessExample name \"" + annotation.name()
                        + "\" on \"" + operation.getOperationId() + "\".");
            }
        }

        ResponseDTO<?> sample = DocExampleFactories.instantiate(annotation.factory());
        JsonNode value = jsonSerializer.toDocExampleJsonNode(sample);
        Example example = new Example().value(value).description(annotation.name());

        content.values().forEach(mediaType -> mediaType.addExamples(annotation.name(), example));
    }

    private String find2xxKey(Operation operation) {
        if (operation.getResponses() == null) {
            throw new IllegalStateException("@ApiSuccessExample on \"" + operation.getOperationId()
                    + "\" but the operation has no responses at all.");
        }
        List<String> twoXx = operation.getResponses().keySet().stream()
                .filter(code -> code.startsWith("2"))
                .toList();
        if (twoXx.size() != 1) {
            throw new IllegalStateException("@ApiSuccessExample on \"" + operation.getOperationId()
                    + "\" expected exactly one 2xx @ApiResponse, found " + twoXx);
        }
        return twoXx.get(0);
    }
}
