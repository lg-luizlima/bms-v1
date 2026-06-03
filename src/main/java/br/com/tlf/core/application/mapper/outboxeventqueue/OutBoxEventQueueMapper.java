package br.com.tlf.core.application.mapper.outboxeventqueue;

import br.com.tlf.infrastructure.persistence.postgresql.entity.OutboxEventQueueEntity;
import br.com.tlf.core.domain.vo.OutBoxEventQueueVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import static br.com.tlf.core.application.ApplicationConstants.AGGREGATE_TYPE;
import static br.com.tlf.core.application.ApplicationConstants.TOPIC_NAME;

@Mapper(componentModel = "spring")
public interface OutBoxEventQueueMapper {

    OutBoxEventQueueMapper INSTANCE = Mappers.getMapper(OutBoxEventQueueMapper.class);


    @Mapping(target = "aggregateId", source = "consentId")
    @Mapping(target = "payload", source = "payloadJson")
    @Mapping(target = "aggregateType", constant = AGGREGATE_TYPE)
    @Mapping(target = "topicName", constant = TOPIC_NAME)
    OutBoxEventQueueVO toVO(String consentId, String payloadJson);

    OutboxEventQueueEntity toOutboxEventQueueEntity(OutBoxEventQueueVO vo);
}
