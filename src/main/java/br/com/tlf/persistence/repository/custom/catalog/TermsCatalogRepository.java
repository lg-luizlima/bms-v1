package br.com.tlf.persistence.repository.custom.catalog;

import java.util.List;

import br.com.tlf.domain.vo.terms.TermsCatalogVO;

public interface TermsCatalogRepository {

    List<TermsCatalogVO> findLatestActiveByProduct(String product);
}



