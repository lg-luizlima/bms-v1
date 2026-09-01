package br.com.tlf.infrastructure.persistence.postgresql.outbox;

import org.mapstruct.Mapper;

import br.com.tlf.core.domain.consent.ConsentRegisteredEvent;
import br.com.tlf.infrastructure.persistence.postgresql.outbox.contract.ConsentRegisteredPayload;

@Mapper(componentModel = "spring")
public interface ConsentRegisteredPayloadMapper {

    ConsentRegisteredPayload toPayload(ConsentRegisteredEvent event);
}
