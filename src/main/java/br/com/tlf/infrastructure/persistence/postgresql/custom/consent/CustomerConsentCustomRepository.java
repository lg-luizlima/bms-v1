package br.com.tlf.infrastructure.persistence.postgresql.custom.consent;

import br.com.tlf.infrastructure.persistence.postgresql.entity.CustomerConsentEntity;
import br.com.tlf.core.domain.vo.terms.CustomerConsentVO;
import br.com.tlf.infrastructure.persistence.postgresql.jpa.CustomerConsentJpaRepository;
import br.com.tlf.infrastructure.persistence.postgresql.mapper.CustomerConsentRepositoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Repository("CustomerConsentCustomRepository")
@Validated
@Primary
public class CustomerConsentCustomRepository implements CustomerConsentRepository {

    private final CustomerConsentJpaRepository customerConsentJpaRepository;


    @Override
    public Boolean getActiveConsent(String cpfHash, String termCode) {
        log.info("Checking active consent for cpfHash={}, termCode={}", cpfHash, termCode);

        var consentActive = customerConsentJpaRepository
                .findActiveByCpfHashAndTermCode(cpfHash, termCode);
        log.info("Active consent query result present: {}", consentActive.isPresent());

        return consentActive.isPresent();
    }

    @Override
    public Optional<UUID> getActiveConsentTermId(String cpfHash, String termCode) {
        log.info("Getting active consent termId for cpfHash={}, termCode={}", cpfHash, termCode);

        return customerConsentJpaRepository
                .findActiveByCpfHashAndTermCode(cpfHash, termCode)
                .map(CustomerConsentEntity::getTermId);
    }

    @Override
    public void saveConsent(CustomerConsentVO consentVO) {
        log.info("Persisting consent for consentVO: {}", consentVO);

        CustomerConsentEntity entity = CustomerConsentRepositoryMapper.INSTANCE.toEntity(consentVO);

        customerConsentJpaRepository.save(entity);
        log.info("Consent saved successfully");
    }
}
