package br.com.tlf.core.port.out.termscatalog;

import java.util.List;
import java.util.UUID;

import br.com.tlf.core.domain.vo.terms.TermsCatalogVO;

public interface TermsCatalogRepository {

    List<TermsCatalogVO> findByIds(List<UUID> termIds);

    List<TermsCatalogVO> findVigentTerms(String product);
}
