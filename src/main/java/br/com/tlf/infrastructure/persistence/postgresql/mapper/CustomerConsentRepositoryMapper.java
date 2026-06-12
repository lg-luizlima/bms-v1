package br.com.tlf.infrastructure.persistence.postgresql.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import br.com.tlf.core.domain.vo.terms.CustomerConsentVO;
import br.com.tlf.infrastructure.persistence.postgresql.entity.CustomerConsentEntity;

@Mapper(componentModel = "spring")
public interface CustomerConsentRepositoryMapper {
    
    @Mapping(target = "id", ignore = true)
    CustomerConsentEntity toEntity(CustomerConsentVO consentVO);
}
