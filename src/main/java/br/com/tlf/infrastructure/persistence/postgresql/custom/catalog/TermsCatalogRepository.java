package br.com.tlf.infrastructure.persistence.postgresql.custom.catalog;

import java.util.List;

import br.com.tlf.core.domain.vo.terms.TermsCatalogVO;

public interface TermsCatalogRepository {

    List<TermsCatalogVO> findLatestActiveByProduct(String product);
}



