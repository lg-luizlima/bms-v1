package br.com.tlf.domain.service.termcatalog;

import br.com.tlf.domain.vo.terms.TermsCatalogVO;

import java.util.List;

public interface TermCatalogService {

    List<TermsCatalogVO> findActiveTerms(String productType);

}