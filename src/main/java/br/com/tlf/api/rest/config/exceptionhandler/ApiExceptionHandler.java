package br.com.tlf.api.rest.config.exceptionhandler;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import br.com.tlf.api.rest.config.exceptionhandler.model.ErrorDetail;
import br.com.tlf.api.rest.config.exceptionhandler.model.ProblemDetailResponse;
import br.com.tlf.core.domain.exception.BusinessException;
import br.com.tlf.core.domain.exception.DomainErrorCode;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String MDC_EXCEPTION_KEY = "exception";
    private static final int MDC_STACK_TRACE_LIMIT = 500;
    private static final String BUSINESS_ERROR_TITLE = "Business Validation Error";
    private static final String BAD_REQUEST_TITLE = "Bad Request Error";


    private static final Map<DomainErrorCode, HttpStatus> STATUS_BY_ERROR_CODE =
            new EnumMap<>(Map.of(
                    DomainErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST,
                    DomainErrorCode.INVALID_CPF_PARAMETER, HttpStatus.BAD_REQUEST,
                    DomainErrorCode.MISSING_CUSTOMER_IDENTIFICATION, HttpStatus.BAD_REQUEST,
                    DomainErrorCode.INVALID_TOKEN, HttpStatus.BAD_REQUEST,
                    DomainErrorCode.PRODUCT_NOT_FOUND, HttpStatus.NOT_FOUND,
                    DomainErrorCode.INVALID_TERM, HttpStatus.UNPROCESSABLE_ENTITY,
                    DomainErrorCode.MANDATORY_TERM_NOT_ACCEPTED, HttpStatus.UNPROCESSABLE_ENTITY,
                    DomainErrorCode.UNEXPECTED_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));

    private static final Map<DomainErrorCode, String> TITLE_BY_ERROR_CODE =
            new EnumMap<>(Map.of(
                    DomainErrorCode.BAD_REQUEST, BAD_REQUEST_TITLE,
                    DomainErrorCode.INVALID_CPF_PARAMETER, BAD_REQUEST_TITLE,
                    DomainErrorCode.MISSING_CUSTOMER_IDENTIFICATION, BAD_REQUEST_TITLE,
                    DomainErrorCode.INVALID_TOKEN, BAD_REQUEST_TITLE,
                    DomainErrorCode.PRODUCT_NOT_FOUND, BUSINESS_ERROR_TITLE,
                    DomainErrorCode.INVALID_TERM, BUSINESS_ERROR_TITLE,
                    DomainErrorCode.MANDATORY_TERM_NOT_ACCEPTED, BUSINESS_ERROR_TITLE,
                    DomainErrorCode.UNEXPECTED_ERROR, "Internal Server Error"));

    private final Tracer tracer;
    private final Clock clock;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetailResponse> handleBusinessException(BusinessException ex) {
        DomainErrorCode errorCode = ex.getErrorCode();
        HttpStatus status = STATUS_BY_ERROR_CODE.getOrDefault(errorCode, HttpStatus.UNPROCESSABLE_ENTITY);

        try (MDC.MDCCloseable ignored = putStackTraceInMdc(ex)) {
            log.error("[ApiExceptionHandler] {}: {}", errorCode, ex.getMessage());

            return ResponseEntity.status(status).body(ProblemDetailResponse.builder()
                    .errorCode(errorCode.getCode())
                    .message(TITLE_BY_ERROR_CODE.getOrDefault(errorCode, BUSINESS_ERROR_TITLE))
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
                    .message(BAD_REQUEST_TITLE)
                    .details("Required field is missing or null inside the request body.")
                    .timestamp(Instant.now(clock).toString())
                    .traceId(currentTraceId())
                    .errors(errors)
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
                    .message("Internal Server Error")
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
}
