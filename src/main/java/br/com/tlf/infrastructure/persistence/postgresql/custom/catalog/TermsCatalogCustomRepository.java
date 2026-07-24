package br.com.tlf.infrastructure.persistence.postgresql.custom.catalog;

import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;

import br.com.tlf.core.domain.vo.terms.TermsCatalogVO;
import br.com.tlf.core.port.out.termscatalog.TermsCatalogRepository;
import br.com.tlf.infrastructure.persistence.postgresql.entity.TermsCatalogJpaEntity;
import br.com.tlf.infrastructure.persistence.postgresql.jpa.TermsCatalogJpaRepository;
import br.com.tlf.infrastructure.persistence.postgresql.mapper.TermsCatalogRepositoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Repository("TermsCatalogCustomRepository")
@Validated
@Primary
public class TermsCatalogCustomRepository implements TermsCatalogRepository {

    private final TermsCatalogJpaRepository termsCatalogJpaRepository;
    private final TermsCatalogRepositoryMapper termsCatalogRepositoryMapper;

    @Override
    public List<TermsCatalogVO> findLatestActiveByProduct(String product) {

        List<TermsCatalogJpaEntity> result = termsCatalogJpaRepository.findLatestActiveByProduct(product);

        return termsCatalogRepositoryMapper.toVO(result);
    }
}
