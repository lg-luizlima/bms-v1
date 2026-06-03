package br.com.tlf.core.domain.service.termcatalog;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import br.com.tlf.core.domain.vo.terms.TermsCatalogVO;
import br.com.tlf.infrastructure.persistence.postgresql.custom.catalog.TermsCatalogRepository;
import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class TermCatalogServiceImpl implements TermCatalogService {

    private final TermsCatalogRepository termsCatalogRepository;

    @Override
    public List<TermsCatalogVO> findActiveTerms(String product) {
        log.info("Fetching active catalog terms for product: {}", product);

        List<TermsCatalogVO> terms = termsCatalogRepository.findLatestActiveByProduct(product);

        log.info("Found {} active terms for product: {}", terms.size(), product);
        return terms;
    }
}
