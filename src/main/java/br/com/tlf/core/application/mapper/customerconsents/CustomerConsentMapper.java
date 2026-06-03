package br.com.tlf.core.application.mapper.customerconsents;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CustomerConsentMapper {

    CustomerConsentMapper INSTANCE = Mappers.getMapper(CustomerConsentMapper.class);


}
