package br.com.tlf.api.rest.config.exceptionhandler;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

import br.com.tlf.core.domain.exception.DomainErrorCode;

/**
 * Single source of truth for {@link DomainErrorCode} metadata — HTTP status, response title and an
 * illustrative example "details" message. Read by {@link ApiExceptionHandler} to build real
 * {@code ProblemDetailResponse}s and by {@code ProblemDetailExampleCustomizer} to generate Swagger
 * examples, so both stay in sync from one place.
 */
public final class DomainErrorRegistry {

    public record DomainErrorMetadata(HttpStatus status, String title, String exampleDetails) {}

    private static final String BAD_REQUEST_TITLE = "Bad Request Error";
    private static final String BUSINESS_ERROR_TITLE = "Business Validation Error";
    private static final String INTERNAL_ERROR_TITLE = "Internal Server Error";

    private static final Map<DomainErrorCode, DomainErrorMetadata> REGISTRY = new EnumMap<>(Map.of(
            DomainErrorCode.BAD_REQUEST, new DomainErrorMetadata(HttpStatus.BAD_REQUEST, BAD_REQUEST_TITLE,
                    "Required field is missing or null inside the request body."),
            DomainErrorCode.INVALID_CPF_PARAMETER, new DomainErrorMetadata(HttpStatus.BAD_REQUEST,
                    BAD_REQUEST_TITLE, "CPF parameter is invalid."),
            DomainErrorCode.MISSING_CUSTOMER_IDENTIFICATION, new DomainErrorMetadata(HttpStatus.BAD_REQUEST,
                    BAD_REQUEST_TITLE, "Customer identification is missing."),
            DomainErrorCode.INVALID_TOKEN, new DomainErrorMetadata(HttpStatus.BAD_REQUEST, BAD_REQUEST_TITLE,
                    "Bearer token is invalid or expired."),
            DomainErrorCode.PRODUCT_NOT_FOUND, new DomainErrorMetadata(HttpStatus.NOT_FOUND, BUSINESS_ERROR_TITLE,
                    "Product not found."),
            DomainErrorCode.INVALID_TERM, new DomainErrorMetadata(HttpStatus.UNPROCESSABLE_ENTITY,
                    BUSINESS_ERROR_TITLE, "Term id is not a valid UUID."),
            DomainErrorCode.MANDATORY_TERM_NOT_ACCEPTED, new DomainErrorMetadata(HttpStatus.UNPROCESSABLE_ENTITY,
                    BUSINESS_ERROR_TITLE, "Mandatory term was not accepted."),
            DomainErrorCode.TERM_NOT_FOUND, new DomainErrorMetadata(HttpStatus.UNPROCESSABLE_ENTITY,
                    BUSINESS_ERROR_TITLE, "Term was not found in the catalog."),
            DomainErrorCode.TERM_OUT_OF_VALIDITY, new DomainErrorMetadata(HttpStatus.UNPROCESSABLE_ENTITY,
                    BUSINESS_ERROR_TITLE, "Term is not vigent at this time."),
            DomainErrorCode.UNEXPECTED_ERROR, new DomainErrorMetadata(HttpStatus.INTERNAL_SERVER_ERROR,
                    INTERNAL_ERROR_TITLE, "An unexpected error occurred. Please try again later.")));

    private DomainErrorRegistry() {}

    public static DomainErrorMetadata metadataFor(DomainErrorCode code) {
        DomainErrorMetadata metadata = REGISTRY.get(code);
        if (metadata == null) {
            throw new IllegalStateException("No DomainErrorRegistry entry for " + code);
        }
        return metadata;
    }
}
