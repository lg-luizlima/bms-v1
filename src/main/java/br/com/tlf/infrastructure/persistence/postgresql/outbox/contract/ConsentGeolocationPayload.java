package br.com.tlf.infrastructure.persistence.postgresql.outbox.contract;


public record ConsentGeolocationPayload(String lat, String lon) {
}
