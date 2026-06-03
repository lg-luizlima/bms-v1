package br.com.tlf.application.mapper.creditcore;

import br.com.tlf.api.lending.rest.v1.dto.request.consent.ConsentRequestDTO;
import br.com.tlf.api.lending.rest.v1.dto.response.consent.ActiveConsentResponseDTO;
import br.com.tlf.domain.vo.consent.ConsentRequestVO;
import br.com.tlf.domain.vo.terms.ActiveConsentResponseVO;
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
