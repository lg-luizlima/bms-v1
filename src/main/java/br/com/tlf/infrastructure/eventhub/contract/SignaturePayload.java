package br.com.tlf.infrastructure.eventhub.contract;

public record SignaturePayload(
        String ip,
        String userAgent,
        String deviceId,
        String channel,
        GeolocationPayload geolocation) {
}
