package br.com.tlf.infrastructure.eventhub.adapter;

import static br.com.tlf.shared.constants.ApplicationConstants.APPLICATION_NAME;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.tlf.core.port.out.eventhub.EventHubPort;
import br.com.tlf.core.port.out.eventhub.dto.request.EventHubRequestDTO;
import br.com.tlf.infrastructure.eventhub.config.EventHubConfig;
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
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        this.producer = new EventHubClientBuilder()
            .connectionString(eventHubConfig.getConnectionString(), eventHubConfig.getEventHubName())
            .buildProducerClient();
    }


    @Override
    public void sendEvent(EventHubRequestDTO request) {
        
        try {
            EventData eventData = new EventData(
                    objectMapper.writeValueAsString(request.getEvent())
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
            producer.close();
        }
    }


}
