package br.com.tlf.infrastructure.eventhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;


@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "features.eventhub")
public class EventHubPublishProperties {

    private boolean parallelPublishEnabled = true;
}
