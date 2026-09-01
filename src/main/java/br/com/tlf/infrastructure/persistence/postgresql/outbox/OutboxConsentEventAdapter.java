package br.com.tlf.infrastructure.persistence.postgresql.outbox;

import static br.com.tlf.shared.constants.ApplicationConstants.AGGREGATE_TYPE;
import static br.com.tlf.shared.constants.ApplicationConstants.TOPIC_NAME;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import br.com.tlf.core.domain.consent.ConsentRegisteredEvent;
import br.com.tlf.core.port.out.outbox.ConsentEventOutbox;
import br.com.tlf.infrastructure.persistence.postgresql.entity.OutboxEventQueueJpaEntity;
import br.com.tlf.infrastructure.persistence.postgresql.jpa.OutboxEventQueueJpaRepository;
import br.com.tlf.shared.util.JsonSerializer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Writes the outbox row that Debezium turns into a Kafka message.
 *
 * <p>The current span's W3C {@code traceparent} is captured into {@code trace_context} in the same
 * transaction as the consent itself; Debezium's outbox router promotes that column to a Kafka
 * header, which is what keeps the worker's consumption a child of the original HTTP request rather
 * than a new trace root.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxConsentEventAdapter implements ConsentEventOutbox {

    private static final String TRACEPARENT = "traceparent";

    private final OutboxEventQueueJpaRepository outboxEventQueueJpaRepository;
    private final ConsentRegisteredPayloadMapper payloadMapper;
    private final JsonSerializer jsonSerializer;
    private final Tracer tracer;
    private final Propagator propagator;

    @Override
    public void publish(String aggregateId, ConsentRegisteredEvent event) {
        log.info("Persisting outbox event, topic: {}, termCode: {}", TOPIC_NAME, event.termCode());

        outboxEventQueueJpaRepository.save(OutboxEventQueueJpaEntity.builder()
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(aggregateId)
                .topicName(TOPIC_NAME)
                .payload(jsonSerializer.toJson(payloadMapper.toPayload(event)))
                .traceContext(currentTraceParent())
                .build());
    }

    private String currentTraceParent() {
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return null;
        }
        Map<String, String> carrier = new HashMap<>();
        propagator.inject(currentSpan.context(), carrier, Map::put);
        return carrier.get(TRACEPARENT);
    }
}
