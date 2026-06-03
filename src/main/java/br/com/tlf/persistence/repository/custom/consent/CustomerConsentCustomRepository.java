package br.com.tlf.persistence.repository.custom.consent;

import br.com.tlf.domain.entity.CustomerConsentEntity;
import br.com.tlf.domain.util.LogUtils;
import br.com.tlf.domain.vo.terms.CustomerConsentVO;
import br.com.tlf.persistence.repository.jpa.CustomerConsentJpaRepository;
import br.com.tlf.persistence.repository.mapper.CustomerConsentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Repository("CustomerConsentCustomRepository")
@Validated
@Primary
public class CustomerConsentCustomRepository implements CustomerConsentRepository {

    private final CustomerConsentJpaRepository customerConsentJpaRepository;

    @Override
    public Boolean getConsentsRevoked(String cpfHash, UUID termId) {
        LogUtils.log("Checking revoked consent for cpfHash={}, termId={}", cpfHash, termId);

        var consentActive = customerConsentJpaRepository
                .findActiveByCpfHashAndTermId(cpfHash, termId);
        LogUtils.log("Revoked consent query result present: {}", consentActive.isPresent());

        return consentActive.isPresent();
    }

    @Override
    public Boolean getActiveConsent(String cpfHash, String termCode) {
        LogUtils.log("Checking active consent for cpfHash={}, termCode={}", cpfHash, termCode);

        var consentActive = customerConsentJpaRepository
                .findActiveByCpfHashAndTermCode(cpfHash, termCode);
        LogUtils.log("Active consent query result present: {}", consentActive.isPresent());

        return consentActive.isPresent();
    }

    @Override
    public void saveConsent(CustomerConsentVO consentVO) {
        LogUtils.log("Persisting consent for consentVO: {}", consentVO);

        CustomerConsentEntity entity = CustomerConsentMapper.INSTANCE.toEntity(consentVO);

        customerConsentJpaRepository.save(entity);
        LogUtils.log("Consent saved successfully");
    }
}
