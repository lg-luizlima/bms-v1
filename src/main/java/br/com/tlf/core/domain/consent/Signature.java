package br.com.tlf.core.domain.consent;

public record Signature(String ip, String userAgent, String deviceId, String channel, Geolocation geolocation) {
}
