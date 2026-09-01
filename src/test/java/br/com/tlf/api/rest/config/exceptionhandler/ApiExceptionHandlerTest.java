package br.com.tlf.api.rest.config.exceptionhandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.context.request.WebRequest;

import br.com.tlf.api.rest.config.exceptionhandler.model.ProblemDetailResponse;
import io.micrometer.tracing.Tracer;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

@ExtendWith(MockitoExtension.class)
class ApiExceptionHandlerTest {

    private static final Instant NOW = Instant.parse("2026-08-31T22:01:21.821317Z");

    @Mock
    private Tracer tracer;

    @Mock
    private Clock clock;

    private ApiExceptionHandler handler;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(NOW);
        handler = new ApiExceptionHandler(tracer, clock);
    }

    private HttpMessageNotReadableException notReadable(Throwable cause) {
        return new HttpMessageNotReadableException("Could not read JSON", cause, null);
    }

    @Test
    void handleHttpMessageNotReadable_booleanTypeMismatch_returnsFieldTypeError() {
        InvalidFormatException cause = InvalidFormatException.from(null, "not a boolean", "oi", Boolean.class);
        cause.prependPath(new Object(), "optIn");

        ResponseEntity<Object> response = handler.handleHttpMessageNotReadable(notReadable(cause),
                new HttpHeaders(), (HttpStatusCode) HttpStatus.BAD_REQUEST, (WebRequest) null);

        ProblemDetailResponse body = (ProblemDetailResponse) response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getErrorCode()).isEqualTo(4000);
        assertThat(body.getDetails()).isEqualTo("Invalid field type in request body.");
        assertThat(body.getErrors()).hasSize(1);
        assertThat(body.getErrors().getFirst().getField()).isEqualTo("optIn");
        assertThat(body.getErrors().getFirst().getMessage()).isEqualTo("Expected Boolean but received String.");
    }

    @Test
    void handleHttpMessageNotReadable_invalidUuid_returnsUuidFormatMessage() {
        InvalidFormatException cause = InvalidFormatException.from(null, "not a uuid", "abc", UUID.class);
        cause.prependPath(new Object(), "termId");

        ResponseEntity<Object> response = handler.handleHttpMessageNotReadable(notReadable(cause),
                new HttpHeaders(), (HttpStatusCode) HttpStatus.BAD_REQUEST, (WebRequest) null);

        ProblemDetailResponse body = (ProblemDetailResponse) response.getBody();
        assertThat(body.getErrors().getFirst().getField()).isEqualTo("termId");
        assertThat(body.getErrors().getFirst().getMessage()).isEqualTo("Expected UUID format.");
    }

    @Test
    void handleHttpMessageNotReadable_unrecognizedProperty_returnsUnrecognizedFieldMessage() {
        UnrecognizedPropertyException cause = new UnrecognizedPropertyException(null,
                "Unrecognized property \"extra\"", null, Object.class, "extra", null);
        cause.prependPath(new Object(), "extra");

        ResponseEntity<Object> response = handler.handleHttpMessageNotReadable(notReadable(cause),
                new HttpHeaders(), (HttpStatusCode) HttpStatus.BAD_REQUEST, (WebRequest) null);

        ProblemDetailResponse body = (ProblemDetailResponse) response.getBody();
        assertThat(body.getErrors().getFirst().getField()).isEqualTo("extra");
        assertThat(body.getErrors().getFirst().getMessage()).isEqualTo("Unrecognized field \"extra\".");
    }

    @Test
    void handleHttpMessageNotReadable_nestedListPath_joinsIndexAndPropertySegments() {
        InvalidFormatException cause = InvalidFormatException.from(null, "not a boolean", "oi", Boolean.class);
        cause.prependPath(new Object(), "optIn");
        cause.prependPath(new Object(), 0);
        cause.prependPath(new Object(), "acceptedTerms");

        ResponseEntity<Object> response = handler.handleHttpMessageNotReadable(notReadable(cause),
                new HttpHeaders(), (HttpStatusCode) HttpStatus.BAD_REQUEST, (WebRequest) null);

        ProblemDetailResponse body = (ProblemDetailResponse) response.getBody();
        assertThat(body.getErrors().getFirst().getField()).isEqualTo("acceptedTerms[0].optIn");
    }

    @Test
    void handleHttpMessageNotReadable_nonJacksonCause_returnsEmptyErrorsList() {
        ResponseEntity<Object> response = handler.handleHttpMessageNotReadable(
                notReadable(new RuntimeException("boom")), new HttpHeaders(), (HttpStatusCode) HttpStatus.BAD_REQUEST,
                (WebRequest) null);

        ProblemDetailResponse body = (ProblemDetailResponse) response.getBody();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getErrorCode()).isEqualTo(4000);
        assertThat(body.getErrors()).isEmpty();
    }
}
