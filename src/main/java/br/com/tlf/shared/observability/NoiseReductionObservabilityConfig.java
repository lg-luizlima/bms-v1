package br.com.tlf.shared.observability;

import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.ObservationFilter;
import io.micrometer.tracing.exporter.SpanExportingPredicate;

@Configuration
@ConditionalOnProperty(prefix = "observability.noise-reduction", name = "health-endpoints-enabled", havingValue = "false")
public class NoiseReductionObservabilityConfig {

    static final String SUPPRESSED_TAG = "noise_reduction_suppressed";

    @Bean
    public ObservationFilter healthEndpointObservationFilter() {
        return context -> {
            if ("true".equals(MDC.get(OtelLogSuppressionFilter.MDC_KEY))) {
                context.addLowCardinalityKeyValue(KeyValue.of(SUPPRESSED_TAG, "true"));
            }
            return context;
        };
    }

    @Bean
    public SpanExportingPredicate healthEndpointSpanExportingPredicate() {
        return finishedSpan -> !"true".equals(finishedSpan.getTags().get(SUPPRESSED_TAG));
    }

    @Bean
    public FilterRegistrationBean<OtelLogSuppressionFilter> otelLogSuppressionFilterRegistration() {
        FilterRegistrationBean<OtelLogSuppressionFilter> registration =
                new FilterRegistrationBean<>(new OtelLogSuppressionFilter());
        registration.addUrlPatterns("/actuator/health", "/actuator/health/*", "/actuator/prometheus");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
