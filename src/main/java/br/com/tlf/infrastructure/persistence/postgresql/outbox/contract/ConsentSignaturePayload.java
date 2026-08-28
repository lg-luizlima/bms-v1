package br.com.tlf.infrastructure.persistence.postgresql.outbox.contract;

public record ConsentSignaturePayload(
        String ip,
        String userAgent,
        String deviceId,
        String channel,
        ConsentGeolocationPayload geolocation) {
}
