package br.com.tlf.api.rest.config.exceptionhandler;

import java.time.Instant;
import java.util.List;
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
import br.com.tlf.core.domain.exception.DomainErrorCode;
import br.com.tlf.core.domain.exception.InvalidCpfParameterException;
import br.com.tlf.core.domain.exception.InvalidTermException;
import br.com.tlf.core.domain.exception.MandatoryTermNotAcceptedException;
import br.com.tlf.core.domain.exception.MissingAuditDataException;
import br.com.tlf.core.domain.exception.ProductNotFoundException;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private final Tracer tracer;

    private String currentTraceId() {
        Span currentSpan = tracer.currentSpan();
        return currentSpan != null ? currentSpan.context().traceId() : UUID.randomUUID().toString();
    }

    private List<ErrorDetail> toErrorDetails(List<String> messages) {
        return messages.stream()
                .map(message -> ErrorDetail.builder().message(message).build())
                .toList();
    }

    @ExceptionHandler(MissingAuditDataException.class)
    public ResponseEntity<ProblemDetailResponse> missingAuditDataException(MissingAuditDataException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        log.error("[ApiExceptionHandler] missing audit data: {}", ex.getMessage());

        String stackTrace = ExceptionUtils.getStackTrace(ex);
        MDC.put("exception", stackTrace.length() > 500 ? stackTrace.substring(0, 500) : stackTrace);

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .errorCode(DomainErrorCode.MISSING_AUDIT_DATA.getCode())
                .message("Validation Error")
                .details("A assinatura de auditoria requer os dados do dispositivo (IP, DeviceId).")
                .timestamp(Instant.now().toString())
                .traceId(currentTraceId())
                .errors(toErrorDetails(ex.getErrors()))
                .build();

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(InvalidTermException.class)
    public ResponseEntity<ProblemDetailResponse> invalidTermException(InvalidTermException ex) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;

        log.error("[ApiExceptionHandler] invalid term: {}", ex.getMessage());

        String stackTrace = ExceptionUtils.getStackTrace(ex);
        MDC.put("exception", stackTrace.length() > 500 ? stackTrace.substring(0, 500) : stackTrace);

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .errorCode(DomainErrorCode.INVALID_TERM.getCode())
                .message("Business Validation Error")
                .details(ex.getMessage())
                .timestamp(Instant.now().toString())
                .traceId(currentTraceId())
                .errors(toErrorDetails(ex.getErrors()))
                .build();

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MandatoryTermNotAcceptedException.class)
    public ResponseEntity<ProblemDetailResponse> mandatoryTermNotAcceptedException(MandatoryTermNotAcceptedException ex) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;

        log.error("[ApiExceptionHandler] mandatory term not accepted: {}", ex.getMessage());

        String stackTrace = ExceptionUtils.getStackTrace(ex);
        MDC.put("exception", stackTrace.length() > 500 ? stackTrace.substring(0, 500) : stackTrace);

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .errorCode(DomainErrorCode.MANDATORY_TERM_NOT_ACCEPTED.getCode())
                .message("Business Validation Error")
                .details(ex.getMessage())
                .timestamp(Instant.now().toString())
                .traceId(currentTraceId())
                .errors(toErrorDetails(ex.getErrors()))
                .build();

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(InvalidCpfParameterException.class)
    public ResponseEntity<ProblemDetailResponse> invalidCpfParameterException(InvalidCpfParameterException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        log.error("[ApiExceptionHandler] invalid cpf parameter: {}", ex.getMessage());

        String stackTrace = ExceptionUtils.getStackTrace(ex);
        MDC.put("exception", stackTrace.length() > 500 ? stackTrace.substring(0, 500) : stackTrace);

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .errorCode(DomainErrorCode.INVALID_CPF_PARAMETER.getCode())
                .message("Bad Request Error")
                .details(ex.getMessage())
                .timestamp(Instant.now().toString())
                .traceId(currentTraceId())
                .errors(toErrorDetails(ex.getErrors()))
                .build();

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ProblemDetailResponse> productNotFoundException(ProductNotFoundException ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;

        log.error("[ApiExceptionHandler] product not found: {}", ex.getMessage());

        String stackTrace = ExceptionUtils.getStackTrace(ex);
        MDC.put("exception", stackTrace.length() > 500 ? stackTrace.substring(0, 500) : stackTrace);

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .errorCode(ex.getErrorCode().getCode())
                .message("Business Validation Error")
                .details(ex.getMessage())
                .timestamp(Instant.now().toString())
                .traceId(currentTraceId())
                .errors(toErrorDetails(ex.getErrors()))
                .build();

        return ResponseEntity.status(status).body(body);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        log.error("[ApiExceptionHandler] request body validation failed: {}", ex.getMessage());

        String stackTrace = ExceptionUtils.getStackTrace(ex);
        MDC.put("exception", stackTrace.length() > 500 ? stackTrace.substring(0, 500) : stackTrace);

        List<ErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> ErrorDetail.builder()
                        .field(fieldError.getField())
                        .message(fieldError.getDefaultMessage())
                        .build())
                .toList();

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .errorCode(DomainErrorCode.BAD_REQUEST.getCode())
                .message("Bad Request Error")
                .details("Required field is missing or null inside the request body.")
                .timestamp(Instant.now().toString())
                .traceId(currentTraceId())
                .errors(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    //Keep this handler as the last one, to catch any unexpected exceptions that may occur in the application
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetailResponse> handleUnexpectedException(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        log.error("[ApiExceptionHandler] unexpected error: {}", ex.getMessage(), ex);

        String stackTrace = ExceptionUtils.getStackTrace(ex);
        MDC.put("exception", stackTrace.length() > 500 ? stackTrace.substring(0, 500) : stackTrace);

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .errorCode(DomainErrorCode.UNEXPECTED_ERROR.getCode())
                .message("Internal Server Error")
                .details("An unexpected error occurred. Please try again later.")
                .timestamp(Instant.now().toString())
                .traceId(currentTraceId())
                .errors(null)
                .build();

        return ResponseEntity.status(status).body(body);
    }

}
