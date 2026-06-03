package br.com.tlf.persistence.repository.custom.catalog;

import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;

import br.com.tlf.domain.entity.TermsCatalogEntity;
import br.com.tlf.domain.util.LogUtils;
import br.com.tlf.domain.vo.terms.TermsCatalogVO;
import br.com.tlf.persistence.repository.jpa.TermsCatalogJpaRepository;
import br.com.tlf.persistence.repository.mapper.TermsCatalogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Repository("TermsCatalogCustomRepository")
@Validated
@Primary
public class TermsCatalogCustomRepository implements TermsCatalogRepository {

    private final TermsCatalogJpaRepository termsCatalogJpaRepository;

    @Override
    public List<TermsCatalogVO> findLatestActiveByProduct(String product) {
        LogUtils.log("Searching latest active catalog terms for product: {}", product);

        List<TermsCatalogEntity> result = termsCatalogJpaRepository.findLatestActiveByProduct(product);

        if (result.isEmpty()) {
            LogUtils.log("No catalog terms found for product: {}", product);
            return List.of();
        }

        List<TermsCatalogVO> vos = TermsCatalogMapper.INSTANCE.toVO(result);

        LogUtils.log("Found {} catalog term(s) for product: {}", vos.size(), product);
        return vos;

    }
}
