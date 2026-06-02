package br.com.tlf.configuration.common.rest.exceptionhandler;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import br.com.tlf.configuration.common.rest.exceptionhandler.error.InvalidTermException;
import br.com.tlf.configuration.common.rest.exceptionhandler.error.MateraIntegrationException;
import br.com.tlf.configuration.common.rest.exceptionhandler.error.MateraIntegrationUnauthorizedException;
import br.com.tlf.configuration.common.rest.exceptionhandler.error.MissingAuditDataException;
import br.com.tlf.configuration.common.rest.exceptionhandler.model.ProblemDetailResponse;
import br.com.tlf.domain.util.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(MateraIntegrationException.class)
    public ResponseEntity<Object> integrationException(Exception ex, WebRequest request) {
        HttpStatus status = HttpStatus.BAD_GATEWAY;
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("type", "about:blank");
        errorResponse.put("title", Objects.nonNull(ex.getMessage()) ? ex.getMessage() : "An error occurred.");
        errorResponse.put("status", status.value());
        errorResponse.put("instance", request.getDescription(false).replace("uri=", ""));
        errorResponse.put("timestamp", Instant.now().toString());
        
        LogUtils.error("[ApiExceptionHandler] error: {}", ex.getMessage());
        
        return ResponseEntity
            .status(status)
            .body(errorResponse);
    }

    @ExceptionHandler(MateraIntegrationUnauthorizedException.class)
    public ResponseEntity<Object> unauthorizedException(Exception ex, WebRequest request) {
        HttpStatus status = HttpStatus.UNAUTHORIZED;
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("type", "about:blank");
        errorResponse.put("title", Objects.nonNull(ex.getMessage()) ? ex.getMessage() : "An error occurred.");
        errorResponse.put("status", status.value());
        errorResponse.put("instance", request.getDescription(false).replace("uri=", ""));
        errorResponse.put("timestamp", Instant.now().toString());
        
        LogUtils.error("[ApiExceptionHandler] error: {}", ex.getMessage());
        
        return ResponseEntity
            .status(status)
            .body(errorResponse);
    }

    @ExceptionHandler(MissingAuditDataException.class)
    public ResponseEntity<ProblemDetailResponse> missingAuditDataException(MissingAuditDataException ex, WebRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        LogUtils.error("[ApiExceptionHandler] missing audit data: {}", ex.getMessage());

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

        LogUtils.error("[ApiExceptionHandler] invalid term: {}", ex.getMessage());

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

}
