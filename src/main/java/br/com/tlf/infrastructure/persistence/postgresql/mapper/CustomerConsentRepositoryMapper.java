package br.com.tlf.infrastructure.persistence.postgresql.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import br.com.tlf.core.domain.vo.terms.CustomerConsentVO;
import br.com.tlf.infrastructure.persistence.postgresql.entity.CustomerConsentJpaEntity;

@Mapper(componentModel = "spring")
public interface CustomerConsentRepositoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cpfHash", source = "customerId")
    CustomerConsentJpaEntity toEntity(CustomerConsentVO consentVO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", source = "cpfHash")
    CustomerConsentVO toVO(CustomerConsentJpaEntity consentEntity);
}
