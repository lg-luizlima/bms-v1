package br.com.tlf.core.domain.service.termcatalog;

import br.com.tlf.core.domain.vo.terms.TermsCatalogVO;

import java.util.List;

public interface TermCatalogService {

    List<TermsCatalogVO> findActiveTerms(String productType);

}