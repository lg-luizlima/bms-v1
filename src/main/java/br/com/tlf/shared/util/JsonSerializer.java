package br.com.tlf.shared.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import org.springframework.stereotype.Component;

@Component
public class JsonSerializer {

    private final JsonMapper jsonMapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

    // Dedicated to OpenAPI doc-example generation only (see ApiSuccessExampleCustomizer) — never used for
    // outbox/Event Hub payloads or persisted audit_details, so disabling WRITE_DATES_AS_TIMESTAMPS here
    // (to match the ISO-8601 format the HTTP layer's Jackson 3 mapper actually returns for Instant fields)
    // cannot change the wire/storage format of any real production payload.
    private final JsonMapper docExampleJsonMapper = JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    public String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize object to JSON", e);
        }
    }

    public JsonNode toJsonNode(Object value) {
        return jsonMapper.valueToTree(value);
    }

    public JsonNode toDocExampleJsonNode(Object value) {
        return docExampleJsonMapper.valueToTree(value);
    }

    public <T> T fromJson(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return jsonMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize " + type.getSimpleName() + " from JSON", e);
        }
    }
}
