package br.com.tlf.core.application.mapper.creditcore;

import br.com.tlf.core.domain.vo.consent.ConsentRequestVO;
import br.com.tlf.core.domain.vo.terms.ActiveConsentResponseVO;
import br.com.tlf.api.rest.dto.request.consent.ConsentRequestDTO;
import br.com.tlf.api.rest.dto.response.consent.ActiveConsentResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CreditCoreMapper {

    CreditCoreMapper INSTANCE = Mappers.getMapper(CreditCoreMapper.class);

    @Mapping(target = "cpf", source = "cpf")
    @Mapping(target = "product", source = "request.product")
    @Mapping(target = "acceptedTerms", source = "request.acceptedTerms")
    @Mapping(target = "signature", source = "request.signature")
    ConsentRequestVO toVO(ConsentRequestDTO request, String cpf);

    ActiveConsentResponseDTO toActiveConsentResponseDTO(ActiveConsentResponseVO activeConsentResponseVO);

}
