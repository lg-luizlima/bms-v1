package br.com.tlf.infrastructure.eventhub.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GeolocationPayload(String lat, @JsonProperty("long") String lon) {
}
