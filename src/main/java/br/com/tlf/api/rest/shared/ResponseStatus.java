package br.com.tlf.api.rest.shared;

import com.fasterxml.jackson.annotation.JsonValue;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Outcome status of a successful response: \"ok\" for read operations, \"success\" for "
        + "operations that changed state", example = "success")
public enum ResponseStatus {

    OK("ok"),
    SUCCESS("success");

    private final String value;

    ResponseStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
