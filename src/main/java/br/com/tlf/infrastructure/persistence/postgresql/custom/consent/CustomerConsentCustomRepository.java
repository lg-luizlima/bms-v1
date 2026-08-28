package br.com.tlf.infrastructure.persistence.postgresql.custom.consent;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import br.com.tlf.core.domain.consent.CustomerConsent;
import br.com.tlf.core.port.out.customerconsent.CustomerConsentRepository;
import br.com.tlf.infrastructure.persistence.postgresql.entity.CustomerConsentJpaEntity;
import br.com.tlf.infrastructure.persistence.postgresql.jpa.CustomerConsentJpaRepository;
import br.com.tlf.infrastructure.persistence.postgresql.mapper.CustomerConsentRepositoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CustomerConsentCustomRepository implements CustomerConsentRepository {

    private final CustomerConsentJpaRepository customerConsentJpaRepository;
    private final CustomerConsentRepositoryMapper customerConsentRepositoryMapper;

    @Override
    public Map<String, CustomerConsent> findActiveConsentsByTermCode(String customerId,
            Collection<String> termCodes) {

        if (termCodes.isEmpty()) {
            return Map.of();
        }

        return customerConsentJpaRepository.findActiveByCpfAndTermCodes(customerId, termCodes).stream()
                .map(customerConsentRepositoryMapper::toDomain)
                .collect(Collectors.toMap(CustomerConsent::termCode, Function.identity(),
                        (newest, older) -> newest, LinkedHashMap::new));
    }

    @Override
    public CustomerConsent save(CustomerConsent consent) {
        log.info("Persisting consent for termCode: {}, termId: {}", consent.termCode(), consent.termId());

        CustomerConsentJpaEntity saved =
                customerConsentJpaRepository.save(customerConsentRepositoryMapper.toEntity(consent));

        return customerConsentRepositoryMapper.toDomain(saved);
    }
}
