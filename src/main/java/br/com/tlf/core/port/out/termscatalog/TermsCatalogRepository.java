package br.com.tlf.core.port.out.termscatalog;

import java.util.List;
import java.util.UUID;

import br.com.tlf.core.domain.terms.TermsCatalog;
import br.com.tlf.core.domain.terms.TermsCatalogEntry;

public interface TermsCatalogRepository {

    List<TermsCatalogEntry> findByIds(List<UUID> termIds);

    TermsCatalog findVigentTerms(String product);
}
