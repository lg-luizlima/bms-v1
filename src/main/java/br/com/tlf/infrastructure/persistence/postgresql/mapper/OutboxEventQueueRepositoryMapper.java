package br.com.tlf.infrastructure.persistence.postgresql.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import br.com.tlf.core.domain.vo.OutBoxEventQueueVO;
import br.com.tlf.infrastructure.persistence.postgresql.entity.OutboxEventQueueJpaEntity;

@Mapper(componentModel = "spring")
public interface OutboxEventQueueRepositoryMapper {

    @Mapping(target = "eventId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    OutboxEventQueueJpaEntity toEntity(OutBoxEventQueueVO vo);
}
