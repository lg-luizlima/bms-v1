package br.com.tlf.application.mapper.creditterm;

import br.com.tlf.domain.vo.terms.ActiveConsentResponseVO;
import br.com.tlf.domain.vo.terms.ConsentDetailsVO;
import br.com.tlf.domain.vo.terms.CustomerConsentVO;
import br.com.tlf.domain.vo.terms.PendingTermVO;
import br.com.tlf.domain.vo.terms.TermsCatalogVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CreditTermMapper {

    CreditTermMapper INSTANCE = Mappers.getMapper(CreditTermMapper.class);

    @Mapping(source = "id",          target = "termId")
    PendingTermVO toPendingTermVO(TermsCatalogVO catalog);

    default ActiveConsentResponseVO toActiveConsentResponseVO(TermsCatalogVO catalog) {
        return ActiveConsentResponseVO.builder()
                .hasPendingMandatoryTerms(Boolean.TRUE)
                .pendingTerms(List.of(toPendingTermVO(catalog)))
                .build();
    }

    @Mapping(source = "id",        target = "consentId")
    @Mapping(source = "termCode",  target = "termId")
    ConsentDetailsVO toConsentDetailsVO(CustomerConsentVO activeConsent);
}
