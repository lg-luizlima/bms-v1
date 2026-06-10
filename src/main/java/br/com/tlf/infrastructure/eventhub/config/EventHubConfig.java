package br.com.tlf.infrastructure.eventhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "azure.eventhub")
public class EventHubConfig {
    private String connectionString;
    private String eventHubName;
    // private String consumerGroup = "$Default";
}
