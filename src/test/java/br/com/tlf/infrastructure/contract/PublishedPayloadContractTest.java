package br.com.tlf.infrastructure.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.tlf.core.domain.consent.ConsentRegisteredEvent;
import br.com.tlf.core.domain.consent.Signature;
import br.com.tlf.infrastructure.eventhub.contract.ConsentRequestedEventMapper;
import br.com.tlf.infrastructure.eventhub.contract.ConsentRequestedEventMapperImpl;
import br.com.tlf.infrastructure.persistence.postgresql.outbox.ConsentRegisteredPayloadMapper;
import br.com.tlf.infrastructure.persistence.postgresql.outbox.ConsentRegisteredPayloadMapperImpl;
import br.com.tlf.dummies.ConsentRequestDummies;
import br.com.tlf.shared.util.JsonSerializer;


class PublishedPayloadContractTest {

    private static final UUID TERM_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private final JsonSerializer jsonSerializer = new JsonSerializer();
    private final ConsentRegisteredPayloadMapper outboxMapper = new ConsentRegisteredPayloadMapperImpl();
    private final ConsentRequestedEventMapper eventHubMapper = new ConsentRequestedEventMapperImpl();

    @Test
    void outboxPayloadKeepsItsPublishedShape() {
        ConsentRegisteredEvent event = ConsentRegisteredEvent.builder()
                .customerId("52998224725")
                .termCode("DATAPREV_AUTH")
                .templateId("template-dataprev-auth-v2")
                .termId(TERM_ID)
                .optIn(Boolean.TRUE)
                .expiresAt("2026-09-27T12:00:00Z")
                .signature(ConsentRequestDummies.signature())
                .build();

        String json = jsonSerializer.toJson(outboxMapper.toPayload(event));

        assertThat(json).isEqualTo("{"
                + "\"customerId\":\"52998224725\","
                + "\"termCode\":\"DATAPREV_AUTH\","
                + "\"templateId\":\"template-dataprev-auth-v2\","
                + "\"termId\":\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\","
                + "\"optIn\":true,"
                + "\"expiresAt\":\"2026-09-27T12:00:00Z\","
                + "\"signature\":{"
                + "\"ip\":\"192.168.0.1\","
                + "\"userAgent\":\"Mozilla/5.0\","
                + "\"deviceId\":\"device-abc-001\","
                + "\"channel\":\"MOBILE\","
                + "\"geolocation\":{\"lat\":\"-23.5505\",\"lon\":\"-46.6333\"}"
                + "}}");
    }

    @Test
    void outboxPayloadKeepsExpiresAtAsExplicitJsonNullForTermsWithNoExpiry() {
        ConsentRegisteredEvent event = ConsentRegisteredEvent.builder()
                .customerId("52998224725")
                .termCode("GENERAL_CREDIT_TERMS")
                .templateId("template-general-credit-terms-v1")
                .termId(TERM_ID)
                .optIn(Boolean.TRUE)
                .expiresAt(null)
                .signature(ConsentRequestDummies.signature())
                .build();

        String json = jsonSerializer.toJson(outboxMapper.toPayload(event));

        assertThat(json).isEqualTo("{"
                + "\"customerId\":\"52998224725\","
                + "\"termCode\":\"GENERAL_CREDIT_TERMS\","
                + "\"templateId\":\"template-general-credit-terms-v1\","
                + "\"termId\":\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\","
                + "\"optIn\":true,"
                + "\"expiresAt\":null,"
                + "\"signature\":{"
                + "\"ip\":\"192.168.0.1\","
                + "\"userAgent\":\"Mozilla/5.0\","
                + "\"deviceId\":\"device-abc-001\","
                + "\"channel\":\"MOBILE\","
                + "\"geolocation\":{\"lat\":\"-23.5505\",\"lon\":\"-46.6333\"}"
                + "}}");
    }

    @Test
    void eventHubPayloadMirrorsTheInboundRequestBody() {
        String json = jsonSerializer.toJson(eventHubMapper.toEvent(
                List.of(ConsentRequestDummies.acceptedRevokedTerm()), ConsentRequestDummies.signature()));

        assertThat(json).isEqualTo("{"
                + "\"acceptedTerms\":[{\"termId\":\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\"optIn\":true}],"
                + "\"signature\":{"
                + "\"ip\":\"192.168.0.1\","
                + "\"userAgent\":\"Mozilla/5.0\","
                + "\"deviceId\":\"device-abc-001\","
                + "\"channel\":\"MOBILE\","
                + "\"geolocation\":{\"lat\":\"-23.5505\",\"long\":\"-46.6333\"}"
                + "}}");
    }

    @Test
    void auditDetailsColumnKeepsItsShapeAndRoundTrips() {
        Signature signature = ConsentRequestDummies.signature();

        String json = jsonSerializer.toJson(signature);

        assertThat(json).isEqualTo("{"
                + "\"ip\":\"192.168.0.1\","
                + "\"userAgent\":\"Mozilla/5.0\","
                + "\"deviceId\":\"device-abc-001\","
                + "\"channel\":\"MOBILE\","
                + "\"geolocation\":{\"lat\":\"-23.5505\",\"lon\":\"-46.6333\"}"
                + "}");
        assertThat(jsonSerializer.fromJson(json, Signature.class)).isEqualTo(signature);
    }
}
