package br.com.tlf.shared.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "observability.pii")
public class ObservabilityPiiProperties {

    private boolean redisValuesEnabled = false;
}
