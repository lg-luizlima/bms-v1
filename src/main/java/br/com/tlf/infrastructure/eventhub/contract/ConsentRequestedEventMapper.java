package br.com.tlf.infrastructure.eventhub.contract;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import br.com.tlf.core.domain.consent.AcceptedTerm;
import br.com.tlf.core.domain.consent.Geolocation;
import br.com.tlf.core.domain.consent.Signature;

@Mapper(componentModel = "spring")
public interface ConsentRequestedEventMapper {

    default ConsentRequestedEvent toEvent(List<AcceptedTerm> acceptedTerms, Signature signature) {
        return new ConsentRequestedEvent(toAcceptedTermPayloads(acceptedTerms), toSignaturePayload(signature));
    }

    List<AcceptedTermPayload> toAcceptedTermPayloads(List<AcceptedTerm> acceptedTerms);

    SignaturePayload toSignaturePayload(Signature signature);


    @Mapping(target = "lon", source = "lon")
    GeolocationPayload toGeolocationPayload(Geolocation geolocation);
}
