package br.com.tlf.core.application.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import br.com.tlf.core.domain.terms.PendingTerm;
import br.com.tlf.core.domain.terms.TermsCatalogEntry;

@Mapper(componentModel = "spring")
public interface TermsCatalogMapper {

    @Mapping(target = "termId", source = "id")
    PendingTerm toPendingTerm(TermsCatalogEntry entry);

    List<PendingTerm> toPendingTerms(List<TermsCatalogEntry> entries);
}
