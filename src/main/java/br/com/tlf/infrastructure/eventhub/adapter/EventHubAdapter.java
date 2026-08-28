package br.com.tlf.infrastructure.eventhub.adapter;

import static br.com.tlf.shared.constants.ApplicationConstants.APPLICATION_NAME;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;

import br.com.tlf.core.domain.consent.AcceptedTerm;
import br.com.tlf.core.domain.consent.Signature;
import br.com.tlf.core.port.out.eventhub.EventHubPort;
import br.com.tlf.infrastructure.eventhub.config.EventHubConfig;
import br.com.tlf.infrastructure.eventhub.config.EventHubPublishProperties;
import br.com.tlf.infrastructure.eventhub.contract.ConsentRequestedEventMapper;
import br.com.tlf.shared.util.JsonSerializer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventHubAdapter implements EventHubPort {

    private static final String EVENT_TYPE = "CREATE_CONSENT_REQUEST";

    private final EventHubConfig eventHubConfig;
    private final EventHubPublishProperties publishProperties;
    private final ConsentRequestedEventMapper eventMapper;
    private final JsonSerializer jsonSerializer;

    private EventHubProducerClient producer;

    @PostConstruct
    public void init() {
        if (!eventHubConfig.isConfigured()) {
            log.warn("EventHub configuration not provided (connection-string or event-hub-name is null). "
                    + "EventHub adapter will not be initialized. This is expected for local development "
                    + "without Azure Event Hub.");
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
    public void publishConsentRequested(List<AcceptedTerm> acceptedTerms, Signature signature) {
        if (!publishProperties.isParallelPublishEnabled()) {
            log.info("[eventHub] Parallel publish disabled. Outbox+Debezium remains the source of event publication.");
            return;
        }

        if (producer == null) {
            log.warn("EventHub producer is not initialized. Event will not be sent. Configure "
                    + "'azure.eventhub.connection-string' and 'azure.eventhub.event-hub-name' to enable publishing.");
            return;
        }

        try {
            EventData eventData = new EventData(
                    jsonSerializer.toJson(eventMapper.toEvent(acceptedTerms, signature)));
            eventData.getProperties().putAll(Map.of("event_type", EVENT_TYPE, "product", APPLICATION_NAME));

            EventDataBatch batch = producer.createBatch();
            batch.tryAdd(eventData);
            producer.send(batch);

            log.info("Event sent to Event Hub, properties: {}", eventData.getProperties());
        } catch (Exception e) {
            log.error("Failed to send event to Event Hub", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (producer == null) {
            return;
        }
        try {
            producer.close();
            log.info("EventHub producer closed successfully");
        } catch (Exception e) {
            log.error("Error closing EventHub producer", e);
        }
    }
}
