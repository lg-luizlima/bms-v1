package br.com.tlf.api.rest.shared;

import com.fasterxml.jackson.annotation.JsonValue;

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
