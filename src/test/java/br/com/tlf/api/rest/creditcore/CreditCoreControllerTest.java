package br.com.tlf.api.rest.creditcore;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.tlf.api.rest.creditcore.mapper.CreditCoreApiMapperImpl;
import br.com.tlf.shared.util.JsonSerializer;
import br.com.tlf.api.rest.creditcore.resolver.CustomerIdResolver;
import br.com.tlf.core.domain.consent.ConsentReceipt;
import br.com.tlf.core.domain.terms.PendingTerm;
import br.com.tlf.core.domain.terms.PendingTermsResult;
import br.com.tlf.core.port.in.CreateConsentPort;
import br.com.tlf.core.port.in.GetPendingTermsPort;
import br.com.tlf.dummies.ConsentRequestDummies;
import br.com.tlf.logging.SensitiveProperties;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;

@WebMvcTest(CreditCoreControllerOpen.class)
@Import({CreditCoreApiMapperImpl.class, CustomerIdResolver.class,
        CreditCoreControllerTest.ObservabilityTestConfig.class})
class CreditCoreControllerTest {

    /**
     * lib-fintech-log's MyFilter is a {@code Filter} bean, so the web slice picks it up and needs its
     * two collaborators. Supplying them keeps the real filter chain in the test.
     */
    @TestConfiguration
    static class ObservabilityTestConfig {

        @Bean
        ObservationRegistry observationRegistry() {
            return ObservationRegistry.create();
        }

        @Bean
        SensitiveProperties sensitiveProperties() {
            SensitiveProperties properties = new SensitiveProperties();
            properties.setMaskEnabled(Boolean.FALSE);
            properties.setFields(new HashSet<>());
            properties.init();
            return properties;
        }
    }

    private static final String CONSENTS_URL = "/credit-core/v1/consents";
    private static final String TERMS_URL = "/credit-core/v1/terms";
    private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");

    /** Boot 4 exposes a Jackson 3 mapper for HTTP; the request body here is built with the house codec. */
    private static final JsonSerializer JSON = new JsonSerializer();

    @Autowired private MockMvc mockMvc;

    @MockitoBean private CreateConsentPort createConsentPort;
    @MockitoBean private GetPendingTermsPort getPendingTermsPort;
    @MockitoBean private Tracer tracer;
    @MockitoBean private Clock clock;

    @BeforeEach
    void freezeClock() {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder consentPost(Object body)
            throws Exception {
        return post(CONSENTS_URL)
                .header("authorization", ConsentRequestDummies.BEARER_TOKEN)
                .header("x-channel-id", ConsentRequestDummies.CHANNEL_ID)
                .header("x-correlation-id", ConsentRequestDummies.CORRELATION_ID)
                .header("x-customer-id", ConsentRequestDummies.CPF_PLAIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.toJson(body));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder consentPostRaw(String rawJson) {
        return post(CONSENTS_URL)
                .header("authorization", ConsentRequestDummies.BEARER_TOKEN)
                .header("x-channel-id", ConsentRequestDummies.CHANNEL_ID)
                .header("x-correlation-id", ConsentRequestDummies.CORRELATION_ID)
                .header("x-customer-id", ConsentRequestDummies.CPF_PLAIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rawJson);
    }

    @Test
    void createConsent_validBody_returns201WithEnvelope() throws Exception {
        when(createConsentPort.execute(any())).thenReturn(new ConsentReceipt(NOW));

        mockMvc.perform(consentPost(ConsentRequestDummies.requestWithMandatoryTerm()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(CreditCoreControllerOpenApi.CREATE_CONSENT_SUCCESS_MESSAGE))
                .andExpect(jsonPath("$.data.consentReceivedAt").exists());
    }

    @Test
    void createConsent_emptyAcceptedTermsAndNullSignature_returns400() throws Exception {
        mockMvc.perform(consentPost(ConsentRequestDummies.requestMissingMandatoryTerm().toBuilder()
                        .signature(null)
                        .build()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(4000))
                .andExpect(jsonPath("$.errors").isArray());

        verify(createConsentPort, never()).execute(any());
    }

    @Test
    void createConsent_signatureMissingDeviceData_returns400() throws Exception {
        var request = ConsentRequestDummies.requestWithMandatoryTerm();
        request.getSignature().setIp("");
        request.getSignature().setDeviceId("");

        mockMvc.perform(consentPost(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(4000));

        verify(createConsentPort, never()).execute(any());
    }

    @Test
    void createConsent_invalidCustomerIdHeader_returns400WithDomainErrorCode() throws Exception {
        mockMvc.perform(post(CONSENTS_URL)
                        .header("authorization", ConsentRequestDummies.BEARER_TOKEN)
                        .header("x-channel-id", ConsentRequestDummies.CHANNEL_ID)
                        .header("x-correlation-id", ConsentRequestDummies.CORRELATION_ID)
                        .header("x-customer-id", "12345678900")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.toJson(ConsentRequestDummies.requestWithMandatoryTerm())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(4002));
    }

    @Test
    void createConsent_optInWrongType_returns400WithFieldError() throws Exception {
        String rawBody = """
                {
                  "acceptedTerms": [
                    {"termId": "%s", "optIn": "oi"}
                  ],
                  "signature": {
                    "ip": "192.168.0.1",
                    "userAgent": "Mozilla/5.0",
                    "deviceId": "device-abc-001",
                    "channel": "MOBILE"
                  }
                }
                """.formatted(java.util.UUID.randomUUID());

        mockMvc.perform(consentPostRaw(rawBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(4000))
                .andExpect(jsonPath("$.errors[0].field").value("acceptedTerms[0].optIn"))
                .andExpect(jsonPath("$.errors[0].message").value("Expected Boolean but received String."));

        verify(createConsentPort, never()).execute(any());
    }

    @Test
    void createConsent_termIdInvalidUuid_returns400WithFieldError() throws Exception {
        String rawBody = """
                {
                  "acceptedTerms": [
                    {"termId": "abc", "optIn": true}
                  ],
                  "signature": {
                    "ip": "192.168.0.1",
                    "userAgent": "Mozilla/5.0",
                    "deviceId": "device-abc-001",
                    "channel": "MOBILE"
                  }
                }
                """;

        mockMvc.perform(consentPostRaw(rawBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(4000))
                .andExpect(jsonPath("$.errors[0].field").value("acceptedTerms[0].termId"))
                .andExpect(jsonPath("$.errors[0].message").value("Expected UUID format."));

        verify(createConsentPort, never()).execute(any());
    }

    @Test
    void getActiveConsents_returns200WithPendingTerms() throws Exception {
        when(getPendingTermsPort.execute(any())).thenReturn(PendingTermsResult.builder()
                .product("CONSIGNADO_DATAPREV")
                .hasPendingMandatoryTerms(true)
                .pendingTerms(List.of(new PendingTerm("term-1", "DATAPREV_AUTH", "Título", "Resumo", null, true)))
                .build());

        mockMvc.perform(get(TERMS_URL)
                        .param("product", "CONSIGNADO_DATAPREV")
                        .header("authorization", ConsentRequestDummies.BEARER_TOKEN)
                        .header("x-channel-id", ConsentRequestDummies.CHANNEL_ID)
                        .header("x-correlation-id", ConsentRequestDummies.CORRELATION_ID)
                        .header("x-customer-id", ConsentRequestDummies.CPF_PLAIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.data.hasPendingMandatoryTerms").value(true))
                .andExpect(jsonPath("$.data.pendingTerms[0].termCode").value("DATAPREV_AUTH"));
    }
}
