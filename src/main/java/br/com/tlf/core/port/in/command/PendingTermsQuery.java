package br.com.tlf.core.port.in.command;

public record PendingTermsQuery(String customerId, String product, String correlationId, String channelId) {
}
