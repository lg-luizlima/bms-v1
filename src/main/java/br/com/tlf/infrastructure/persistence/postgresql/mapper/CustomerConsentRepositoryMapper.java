package br.com.tlf.infrastructure.persistence.postgresql.mapper;

import br.com.tlf.infrastructure.persistence.postgresql.entity.CustomerConsentEntity;
import br.com.tlf.core.domain.vo.terms.CustomerConsentVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CustomerConsentRepositoryMapper {
    
    CustomerConsentRepositoryMapper INSTANCE = Mappers.getMapper(CustomerConsentRepositoryMapper.class);

    CustomerConsentEntity toEntity(CustomerConsentVO consentVO);
}
