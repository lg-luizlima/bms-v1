package br.com.tlf.domain.service.termcatalog;

import static br.com.tlf.application.ApplicationConstants.CLASS_METHOD_MESSAGE_PATTERN;

import java.util.List;

import org.springframework.stereotype.Service;
import br.com.tlf.domain.util.LogUtils;
import br.com.tlf.domain.vo.terms.TermsCatalogVO;
import br.com.tlf.persistence.repository.custom.catalog.TermsCatalogRepository;
import br.com.tlf.persistence.repository.custom.consent.CustomerConsentRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TermCatalogServiceImpl implements TermCatalogService {

    private final TermsCatalogRepository termsCatalogRepository;


    @Override
    public List<TermsCatalogVO> findActiveTerms(String product) {
        LogUtils.log(CLASS_METHOD_MESSAGE_PATTERN, this.getClass().getSimpleName(), "findActiveTerms");
        LogUtils.log("Fetching active catalog terms for product: {}", product);
        List<TermsCatalogVO> terms = termsCatalogRepository.findLatestActiveByProduct(product);
        LogUtils.log("Found {} active terms for product: {}", terms.size(), product);
        return terms;
    }
}
