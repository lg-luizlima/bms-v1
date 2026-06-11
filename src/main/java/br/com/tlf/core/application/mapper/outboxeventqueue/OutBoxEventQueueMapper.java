package br.com.tlf.core.application.mapper.outboxeventqueue;

import static br.com.tlf.shared.constants.ApplicationConstants.AGGREGATE_TYPE;
import static br.com.tlf.shared.constants.ApplicationConstants.TOPIC_NAME;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import br.com.tlf.infrastructure.persistence.postgresql.entity.OutboxEventQueueEntity;

@Mapper(componentModel = "spring")
public interface OutBoxEventQueueMapper {

    @Mapping(target = "aggregateId", source = "consentId")
    @Mapping(target = "payload", source = "payloadJson")
    @Mapping(target = "aggregateType", constant = AGGREGATE_TYPE)
    @Mapping(target = "topicName", constant = TOPIC_NAME)
    OutboxEventQueueEntity toEntity(String consentId, String payloadJson);

    // OutboxEventQueueEntity toOutboxEventQueueEntity(OutBoxEventQueueVO vo);
}
