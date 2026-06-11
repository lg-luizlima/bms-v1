package br.com.tlf.core.port.out.termscatalog;

import java.util.List;

import br.com.tlf.core.domain.vo.terms.TermsCatalogVO;

public interface TermsCatalogRepository {

    List<TermsCatalogVO> findLatestActiveByProduct(String product);
}



