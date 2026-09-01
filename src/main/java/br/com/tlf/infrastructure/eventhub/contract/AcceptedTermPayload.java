package br.com.tlf.infrastructure.eventhub.contract;

public record AcceptedTermPayload(String termId, Boolean optIn) {
}
