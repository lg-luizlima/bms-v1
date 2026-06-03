package br.com.tlf.persistence.repository.mapper;

import br.com.tlf.domain.entity.CustomerConsentEntity;
import br.com.tlf.domain.vo.terms.CustomerConsentVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CustomerConsentMapper {
    
    CustomerConsentMapper INSTANCE = Mappers.getMapper(CustomerConsentMapper.class);

    CustomerConsentEntity toEntity(CustomerConsentVO consentVO);
}
