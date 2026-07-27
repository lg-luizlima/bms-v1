package br.com.tlf.shared.observability;

import org.springframework.context.annotation.Configuration;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;


@Configuration
@RequiredArgsConstructor
public class OpenTelemetryLoggingConfig {

    private final OpenTelemetry openTelemetry;

    @PostConstruct
    public void installOpenTelemetryAppender() {
        OpenTelemetryAppender.install(openTelemetry);
    }
}
