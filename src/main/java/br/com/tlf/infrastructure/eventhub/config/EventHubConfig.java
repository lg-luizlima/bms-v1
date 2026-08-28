package br.com.tlf.infrastructure.eventhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "azure.eventhub")
public class EventHubConfig {

    private String connectionString;
    private String eventHubName;

    public boolean isConfigured() {
        return connectionString != null && eventHubName != null;
    }
}
