package br.com.tlf.api.rest.config.exceptionhandler;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import br.com.tlf.api.rest.config.exceptionhandler.DomainErrorRegistry.DomainErrorMetadata;
import br.com.tlf.api.rest.config.exceptionhandler.model.ErrorDetail;
import br.com.tlf.api.rest.config.exceptionhandler.model.ProblemDetailResponse;
import br.com.tlf.core.domain.exception.BusinessException;
import br.com.tlf.core.domain.exception.DomainErrorCode;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String MDC_EXCEPTION_KEY = "exception";
    private static final int MDC_STACK_TRACE_LIMIT = 500;

    /** Shared with {@code BadRequestValidationExample} so its Swagger example can't drift from this text. */
    public static final String MISSING_REQUIRED_FIELD_DETAILS =
            "Required field is missing or null inside the request body.";

    private final Tracer tracer;
    private final Clock clock;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetailResponse> handleBusinessException(BusinessException ex) {
        DomainErrorCode errorCode = ex.getErrorCode();
        DomainErrorMetadata metadata = DomainErrorRegistry.metadataFor(errorCode);

        try (MDC.MDCCloseable ignored = putStackTraceInMdc(ex)) {
            log.error("[ApiExceptionHandler] {}: {}", errorCode, ex.getMessage());

            return ResponseEntity.status(metadata.status()).body(ProblemDetailResponse.builder()
                    .errorCode(errorCode.getCode())
                    .message(metadata.title())
                    .details(ex.getMessage())
                    .timestamp(Instant.now(clock).toString())
                    .traceId(currentTraceId())
                    .errors(toErrorDetails(ex.getErrors()))
                    .build());
        }
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        try (MDC.MDCCloseable ignored = putStackTraceInMdc(ex)) {
            log.error("[ApiExceptionHandler] request body validation failed: {}", ex.getMessage());

            List<ErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                    .map(fieldError -> ErrorDetail.builder()
                            .field(fieldError.getField())
                            .message(fieldError.getDefaultMessage())
                            .build())
                    .toList();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ProblemDetailResponse.builder()
                    .errorCode(DomainErrorCode.BAD_REQUEST.getCode())
                    .message(DomainErrorRegistry.metadataFor(DomainErrorCode.BAD_REQUEST).title())
                    .details(MISSING_REQUIRED_FIELD_DETAILS)
                    .timestamp(Instant.now(clock).toString())
                    .traceId(currentTraceId())
                    .errors(errors)
                    .build());
        }
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        try (MDC.MDCCloseable ignored = putStackTraceInMdc(ex)) {
            log.error("[ApiExceptionHandler] request body could not be read: {}", ex.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ProblemDetailResponse.builder()
                    .errorCode(DomainErrorCode.BAD_REQUEST.getCode())
                    .message(DomainErrorRegistry.metadataFor(DomainErrorCode.BAD_REQUEST).title())
                    .details("Invalid field type in request body.")
                    .timestamp(Instant.now(clock).toString())
                    .traceId(currentTraceId())
                    .errors(toInvalidFormatErrors   (ex))
                    .build());
        }
    }

    // Keep this handler as the last one, to catch any unexpected exceptions that may occur in the application
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetailResponse> handleUnexpectedException(Exception ex) {
        try (MDC.MDCCloseable ignored = putStackTraceInMdc(ex)) {
            log.error("[ApiExceptionHandler] unexpected error: {}", ex.getMessage(), ex);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ProblemDetailResponse.builder()
                    .errorCode(DomainErrorCode.UNEXPECTED_ERROR.getCode())
                    .message(DomainErrorRegistry.metadataFor(DomainErrorCode.UNEXPECTED_ERROR).title())
                    .details("An unexpected error occurred. Please try again later.")
                    .timestamp(Instant.now(clock).toString())
                    .traceId(currentTraceId())
                    .errors(null)
                    .build());
        }
    }


    private MDC.MDCCloseable putStackTraceInMdc(Exception ex) {
        String stackTrace = ExceptionUtils.getStackTrace(ex);
        return MDC.putCloseable(MDC_EXCEPTION_KEY, stackTrace.length() > MDC_STACK_TRACE_LIMIT
                ? stackTrace.substring(0, MDC_STACK_TRACE_LIMIT)
                : stackTrace);
    }

    private String currentTraceId() {
        Span currentSpan = tracer.currentSpan();
        return currentSpan != null ? currentSpan.context().traceId() : UUID.randomUUID().toString();
    }

    private List<ErrorDetail> toErrorDetails(List<String> messages) {
        return messages.stream()
                .map(message -> ErrorDetail.builder().message(message).build())
                .toList();
    }

    private List<ErrorDetail> toInvalidFormatErrors(HttpMessageNotReadableException ex) {
        if (ex.getCause() instanceof MismatchedInputException cause) {
            return List.of(ErrorDetail.builder()
                    .field(fieldPath(cause.getPath()))
                    .message(formatMessage(cause))
                    .build());
        }
        return List.of();
    }

    private String fieldPath(List<JacksonException.Reference> path) {
        StringBuilder builder = new StringBuilder();
        for (JacksonException.Reference reference : path) {
            if (reference.getIndex() >= 0) {
                builder.append('[').append(reference.getIndex()).append(']');
            } else if (reference.getPropertyName() != null) {
                if (!builder.isEmpty()) {
                    builder.append('.');
                }
                builder.append(reference.getPropertyName());
            }
        }
        return builder.toString();
    }

    private String formatMessage(MismatchedInputException cause) {
        if (cause instanceof UnrecognizedPropertyException unrecognized) {
            return "Unrecognized field \"" + unrecognized.getPropertyName() + "\".";
        }

        Class<?> targetType = cause.getTargetType();
        if (cause instanceof InvalidFormatException invalidFormat) {
            if (UUID.class.equals(targetType)) {
                return "Expected UUID format.";
            }
            String expectedType = targetType != null ? targetType.getSimpleName() : "value";
            Object value = invalidFormat.getValue();
            String receivedType = value != null ? value.getClass().getSimpleName() : "null";
            return "Expected %s but received %s.".formatted(expectedType, receivedType);
        }

        return "Malformed value in request body.";
    }
}
