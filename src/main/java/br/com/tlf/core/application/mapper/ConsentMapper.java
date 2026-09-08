package br.com.tlf.core.application.mapper;

import java.time.Instant;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import br.com.tlf.core.domain.consent.AcceptedTerm;
import br.com.tlf.core.domain.consent.ConsentRegisteredEvent;
import br.com.tlf.core.domain.consent.CustomerConsent;
import br.com.tlf.core.domain.consent.Signature;
import br.com.tlf.core.domain.terms.TermsCatalogEntry;
import br.com.tlf.core.port.in.command.CreateConsentCommand;

@Mapper(componentModel = "spring")
public interface ConsentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", source = "command.customerId")
    @Mapping(target = "termCode", source = "term.termCode")
    @Mapping(target = "termId", source = "term.id")
    @Mapping(target = "optIn", source = "acceptedTerm.optIn")
    @Mapping(target = "acceptedAt", source = "acceptedAt")
    @Mapping(target = "expiresAt", expression = "java(term.expiryFrom(acceptedAt))")
    @Mapping(target = "signature", source = "command.signature")
    CustomerConsent toCustomerConsent(CreateConsentCommand command, TermsCatalogEntry term,
            AcceptedTerm acceptedTerm, Instant acceptedAt);

    @Mapping(target = "customerId", source = "consent.customerId")
    @Mapping(target = "termCode", source = "consent.termCode")
    @Mapping(target = "templateId", source = "term.templateId")
    @Mapping(target = "termId", source = "consent.id")
    @Mapping(target = "optIn", source = "consent.optIn")
    @Mapping(target = "expiresAt", expression = "java(consent.expiresAt() == null ? null : consent.expiresAt().toString())")
    @Mapping(target = "signature", source = "signature")
    ConsentRegisteredEvent toEvent(CustomerConsent consent, TermsCatalogEntry term, Signature signature);
}
