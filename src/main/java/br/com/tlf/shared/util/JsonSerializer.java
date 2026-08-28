package br.com.tlf.shared.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import org.springframework.stereotype.Component;

@Component
public class JsonSerializer {

    private final JsonMapper jsonMapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

    public String toJson(Object value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize object to JSON", e);
        }
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
