package br.com.tlf.core.port.out.eventhub;

import java.util.List;

import br.com.tlf.core.domain.consent.AcceptedTerm;
import br.com.tlf.core.domain.consent.Signature;


public interface EventHubPort {

    void publishConsentRequested(List<AcceptedTerm> acceptedTerms, Signature signature);
}
