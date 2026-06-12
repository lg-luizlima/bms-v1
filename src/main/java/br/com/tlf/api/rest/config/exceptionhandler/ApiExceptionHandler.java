package br.com.tlf.api.rest.config.exceptionhandler;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import br.com.tlf.api.rest.config.exceptionhandler.model.ProblemDetailResponse;
import br.com.tlf.core.domain.exception.InvalidTermException;
import br.com.tlf.core.domain.exception.MandatoryTermNotAcceptedException;
import br.com.tlf.core.domain.exception.MissingAuditDataException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(MissingAuditDataException.class)
    public ResponseEntity<ProblemDetailResponse> missingAuditDataException(MissingAuditDataException ex, WebRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        log.error("[ApiExceptionHandler] missing audit data: {}", ex.getMessage());

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .errorCode(status.value())
                .message("Validation Error")
                .details("A assinatura de auditoria requer os dados do dispositivo (IP, DeviceId).")
                .timestamp(Instant.now().toString())
                .traceId(UUID.randomUUID().toString())
                .errors(ex.getErrors())
                .build();

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(InvalidTermException.class)
    public ResponseEntity<ProblemDetailResponse> invalidTermException(InvalidTermException ex, WebRequest request) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;

        log.error("[ApiExceptionHandler] invalid term: {}", ex.getMessage());

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .errorCode(status.value())
                .message("Business Validation Error")
                .details(ex.getMessage())
                .timestamp(Instant.now().toString())
                .traceId(UUID.randomUUID().toString())
                .errors(ex.getErrors())
                .build();

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MandatoryTermNotAcceptedException.class)
    public ResponseEntity<ProblemDetailResponse> mandatoryTermNotAcceptedException(MandatoryTermNotAcceptedException ex, WebRequest request) {
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;

        log.error("[ApiExceptionHandler] mandatory term not accepted: {}", ex.getMessage());

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .errorCode(status.value())
                .message("Business Validation Error")
                .details(ex.getMessage())
                .timestamp(Instant.now().toString())
                .traceId(UUID.randomUUID().toString())
                .errors(ex.getErrors())
                .build();

        return ResponseEntity.status(status).body(body);
    }
    //Keep this handler as the last one, to catch any unexpected exceptions that may occur in the application
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetailResponse> handleUnexpectedException(Exception ex, WebRequest request) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        log.error("[ApiExceptionHandler] unexpected error: {}", ex.getMessage(), ex);

        ProblemDetailResponse body = ProblemDetailResponse.builder()
                .errorCode(status.value())
                .message("Internal Server Error")
                .details("An unexpected error occurred. Please try again later.")
                .timestamp(Instant.now().toString())
                .traceId(UUID.randomUUID().toString())
                .errors(null)
                .build();

        return ResponseEntity.status(status).body(body);
    }

}
