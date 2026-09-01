package br.com.tlf.api.rest.creditcore.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import br.com.tlf.api.rest.creditcore.dto.request.ConsentRequestDTO;
import br.com.tlf.api.rest.creditcore.dto.request.GeolocationDTO;
import br.com.tlf.api.rest.creditcore.dto.response.ActiveConsentResponseDTO;
import br.com.tlf.api.rest.creditcore.dto.response.ConsentResponseDTO;
import br.com.tlf.core.domain.consent.ConsentReceipt;
import br.com.tlf.core.domain.consent.Geolocation;
import br.com.tlf.core.domain.terms.PendingTermsResult;
import br.com.tlf.core.port.in.command.CreateConsentCommand;

@Mapper(componentModel = "spring")
public interface CreditCoreApiMapper {

    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "correlationId", source = "correlationId")
    @Mapping(target = "channelId", source = "channelId")
    @Mapping(target = "acceptedTerms", source = "request.acceptedTerms")
    @Mapping(target = "signature", source = "request.signature")
    CreateConsentCommand toCommand(ConsentRequestDTO request, String customerId, String correlationId,
            String channelId);

    @Mapping(target = "lon", source = "lon")
    Geolocation toGeolocation(GeolocationDTO geolocationDTO);

    ActiveConsentResponseDTO toResponse(PendingTermsResult result);

    List<ActiveConsentResponseDTO> toResponse(List<PendingTermsResult> results);

    ConsentResponseDTO toResponse(ConsentReceipt receipt);
}
