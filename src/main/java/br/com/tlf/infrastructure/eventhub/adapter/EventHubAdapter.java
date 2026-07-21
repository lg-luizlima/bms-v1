package br.com.tlf.infrastructure.eventhub.adapter;

import static br.com.tlf.shared.constants.ApplicationConstants.APPLICATION_NAME;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;

import br.com.tlf.core.port.out.eventhub.EventHubPort;
import br.com.tlf.core.port.out.eventhub.dto.request.EventHubRequestDTO;
import br.com.tlf.infrastructure.eventhub.config.EventHubConfig;
import br.com.tlf.shared.util.JsonSerializer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventHubAdapter implements EventHubPort {

    private final EventHubConfig eventHubConfig;
    private EventHubProducerClient producer;
    private final JsonSerializer jsonSerializer;

    @PostConstruct
    public void init() {
        if (eventHubConfig.getConnectionString() == null || eventHubConfig.getEventHubName() == null) {
            log.warn("EventHub configuration not provided (connection-string or event-hub-name is null). EventHub adapter will not be initialized. This is expected for local development without Azure Event Hub.");
            return;
        }

        try {
            this.producer = new EventHubClientBuilder()
                .connectionString(eventHubConfig.getConnectionString(), eventHubConfig.getEventHubName())
                .buildProducerClient();
            log.info("EventHub adapter initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize EventHub adapter", e);
        }
    }


    @Override
    public void sendEvent(EventHubRequestDTO request) {
        if (producer == null) {
            log.warn("EventHub producer is not initialized. Event will not be sent. Configure 'azure.eventhub.connection-string' and 'azure.eventhub.event-hub-name' to enable Event Hub publishing.");
            return;
        }

        try {
            EventData eventData = new EventData(
                    jsonSerializer.toJson(request.getEvent())
            );
            eventData.getProperties().putAll(Map.of("event_type", request.getEventType(), "product", APPLICATION_NAME));
            EventDataBatch batch = producer.createBatch();
            batch.tryAdd(eventData);
            producer.send(batch);
            log.info("Event sent to Event Hub: {}, properties: {}", eventData.getBodyAsString(), eventData.getProperties());
        } catch (Exception e) {
            log.error("Failed to send event to Event Hub", e);
        }

    }

    @PreDestroy
    public void cleanup() {
        if (producer != null) {
            try {
                producer.close();
                log.info("EventHub producer closed successfully");
            } catch (Exception e) {
                log.error("Error closing EventHub producer", e);
            }
        }
    }


}
