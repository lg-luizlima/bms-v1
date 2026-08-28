package br.com.tlf.infrastructure.persistence.postgresql.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import br.com.tlf.core.domain.terms.TermsCatalogEntry;
import br.com.tlf.infrastructure.persistence.postgresql.entity.TermsCatalogJpaEntity;

@Mapper(componentModel = "spring")
public interface TermsCatalogRepositoryMapper {

    TermsCatalogEntry toDomain(TermsCatalogJpaEntity entity);

    List<TermsCatalogEntry> toDomain(List<TermsCatalogJpaEntity> entities);
}
