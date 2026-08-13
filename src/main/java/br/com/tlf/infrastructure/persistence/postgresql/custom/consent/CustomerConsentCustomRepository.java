package br.com.tlf.infrastructure.persistence.postgresql.custom.consent;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;

import br.com.tlf.core.domain.vo.terms.CustomerConsentVO;
import br.com.tlf.core.port.out.customerconsent.CustomerConsentRepository;
import br.com.tlf.infrastructure.persistence.postgresql.entity.CustomerConsentJpaEntity;
import br.com.tlf.infrastructure.persistence.postgresql.jpa.CustomerConsentJpaRepository;
import br.com.tlf.infrastructure.persistence.postgresql.mapper.CustomerConsentRepositoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Repository("CustomerConsentCustomRepository")
@Validated
@Primary
public class CustomerConsentCustomRepository implements CustomerConsentRepository {

    private final CustomerConsentJpaRepository customerConsentJpaRepository;
    private final CustomerConsentRepositoryMapper customerConsentRepositoryMapper;

    @Override
    public CustomerConsentVO getActiveCustomerConsent(String cpf, String termCode) {
        log.info("Getting active consent termId for cpf={}, termCode={}", cpf, termCode);

        return customerConsentJpaRepository
                .findActiveByCpfAndTermCode(cpf, termCode)
                .map(customerConsentRepositoryMapper::toVO)
                .orElse(null);
    }

    @Override
    public CustomerConsentVO saveConsent(CustomerConsentVO consentVO) {
        log.info("Persisting consent for consentVO: {}", consentVO);

        CustomerConsentJpaEntity entity = customerConsentRepositoryMapper.toEntity(consentVO);

        customerConsentJpaRepository.save(entity);
        CustomerConsentVO savedConsent = customerConsentRepositoryMapper.toVO(entity);
        return savedConsent;
    }
}
