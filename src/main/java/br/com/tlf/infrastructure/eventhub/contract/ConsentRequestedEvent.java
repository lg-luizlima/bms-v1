package br.com.tlf.infrastructure.eventhub.contract;

import java.util.List;


public record ConsentRequestedEvent(List<AcceptedTermPayload> acceptedTerms, SignaturePayload signature) {
}
