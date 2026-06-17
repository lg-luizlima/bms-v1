package br.com.tlf.core.application.mapper.creditcore;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.tlf.core.domain.vo.consent.AcceptedTermVO;
import br.com.tlf.core.domain.vo.consent.ConsentRequestVO;
import br.com.tlf.core.domain.vo.terms.ActiveConsentResponseVO;
import br.com.tlf.core.domain.vo.terms.CustomerConsentVO;
import br.com.tlf.core.domain.vo.terms.PendingTermVO;
import br.com.tlf.core.domain.vo.terms.TermsCatalogVO;
import br.com.tlf.core.port.in.dto.request.ConsentRequestDTO;
import br.com.tlf.core.port.in.dto.response.ActiveConsentResponseDTO;
import br.com.tlf.shared.constants.ApplicationConstants;

@Mapper(componentModel = "spring")
public interface CreditCoreMapper {

    CreditCoreMapper INSTANCE = Mappers.getMapper(CreditCoreMapper.class);
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mapping(target = "cpf", source = "cpf")
    @Mapping(target = "product", source = "request.product")
    @Mapping(target = "acceptedTerms", source = "request.acceptedTerms")
    @Mapping(target = "signature", source = "request.signature")
    ConsentRequestVO toVO(ConsentRequestDTO request, String cpf);

    @Mapping(target = "termId", source = "id")
    PendingTermVO toPendingTermVO(TermsCatalogVO catalog);

    List<PendingTermVO> toPendingTermVO(List<TermsCatalogVO> catalog);

    ActiveConsentResponseDTO toActiveConsentResponseDTO(ActiveConsentResponseVO activeConsentResponseVO);

    @Mapping(target = "cpfHash", source = "cpfHash")
    @Mapping(target = "termCode", source = "acceptedTerm.termCode")
    @Mapping(target = "termId", source = "termCatalog.id")
    @Mapping(target = "optIn", source = "acceptedTerm.optIn")
    @Mapping(target = "acceptedAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "expiresAt", expression = "java(calculateExpiresAt(termCatalog))")
    @Mapping(target = "auditDetails", expression = "java(serializeAuditDetails(consentRequestVO))")
    CustomerConsentVO toCustomerConsentVO(String cpfHash, ConsentRequestVO consentRequestVO, AcceptedTermVO acceptedTerm, TermsCatalogVO termCatalog);

    default Instant calculateExpiresAt(TermsCatalogVO termCatalog) {
        long days = termCatalog.getValidityDays() != null
                ? termCatalog.getValidityDays()
                : ApplicationConstants.MAX_VALIDITY_DAYS;
        return Instant.now().plus(days, ChronoUnit.DAYS);
    }

    default String serializeAuditDetails(ConsentRequestVO consentRequestVO) {
        try {
            return OBJECT_MAPPER.writeValueAsString(consentRequestVO.getSignature());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
