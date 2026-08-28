package br.com.tlf.core.port.in.command;

import java.util.List;

import br.com.tlf.core.domain.consent.AcceptedTerm;
import br.com.tlf.core.domain.consent.Signature;


public record CreateConsentCommand(
        String customerId,
        String correlationId,
        String channelId,
        List<AcceptedTerm> acceptedTerms,
        Signature signature) {
}
