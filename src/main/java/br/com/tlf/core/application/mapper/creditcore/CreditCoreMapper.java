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
import br.com.tlf.core.domain.vo.consent.SignatureVO;
import br.com.tlf.core.domain.vo.terms.ActiveConsentResponseVO;
import br.com.tlf.core.domain.vo.terms.ConsentEventPayloadVO;
import br.com.tlf.core.domain.vo.terms.ConsentSignaturePayloadVO;
import br.com.tlf.core.domain.vo.terms.CustomerConsentVO;
import br.com.tlf.core.domain.vo.terms.PendingTermVO;
import br.com.tlf.core.domain.vo.terms.TermsCatalogVO;
import br.com.tlf.core.port.in.dto.request.ConsentRequestDTO;
import br.com.tlf.core.port.in.dto.response.ActiveConsentResponseDTO;

@Mapper(componentModel = "spring")
public interface CreditCoreMapper {

    CreditCoreMapper INSTANCE = Mappers.getMapper(CreditCoreMapper.class);
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mapping(target = "customerId", source = "customerId")
    @Mapping(target = "acceptedTerms", source = "request.acceptedTerms")
    @Mapping(target = "signature", source = "request.signature")
    ConsentRequestVO toVO(ConsentRequestDTO request, String customerId);

    @Mapping(target = "termId", source = "id")
    PendingTermVO toPendingTermVO(TermsCatalogVO catalog);

    List<PendingTermVO> toPendingTermVO(List<TermsCatalogVO> catalog);

    ActiveConsentResponseDTO toActiveConsentResponseDTO(ActiveConsentResponseVO activeConsentResponseVO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", source = "cpfToken")
    @Mapping(target = "termCode", source = "termCatalog.termCode")
    @Mapping(target = "termId", source = "termCatalog.id")
    @Mapping(target = "optIn", source = "acceptedTerm.optIn")
    @Mapping(target = "acceptedAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "expiresAt", expression = "java(calculateExpiresAt(termCatalog))")
    @Mapping(target = "auditDetails", expression = "java(serializeAuditDetails(consentRequestVO))")
    CustomerConsentVO toCustomerConsentVO(String cpfToken, ConsentRequestVO consentRequestVO, AcceptedTermVO acceptedTerm, TermsCatalogVO termCatalog);

    ConsentSignaturePayloadVO toConsentSignaturePayloadVO(SignatureVO signature);

    @Mapping(target = "customerId", source = "cpf")
    @Mapping(target = "termCode", source = "consent.termCode")
    @Mapping(target = "templateId", source = "termCatalog.templateId")
    @Mapping(target = "termId", source = "consent.termId")
    @Mapping(target = "optIn", source = "consent.optIn")
    @Mapping(target = "expiresAt", expression = "java(consent.getExpiresAt() == null ? null : consent.getExpiresAt().toString())")
    @Mapping(target = "signature", source = "signature")
    ConsentEventPayloadVO toConsentEventPayloadVO(String cpf, CustomerConsentVO consent, TermsCatalogVO termCatalog, SignatureVO signature);

    default Instant calculateExpiresAt(TermsCatalogVO termCatalog) {
        if (termCatalog.getValidityDays() == null) {
            return null;
        }
        return Instant.now().plus(termCatalog.getValidityDays(), ChronoUnit.DAYS);
    }

    default String serializeAuditDetails(ConsentRequestVO consentRequestVO) {
        try {
            return OBJECT_MAPPER.writeValueAsString(consentRequestVO.getSignature());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
