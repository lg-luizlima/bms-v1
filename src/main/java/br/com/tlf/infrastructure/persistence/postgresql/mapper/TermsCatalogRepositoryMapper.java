package br.com.tlf.infrastructure.persistence.postgresql.mapper;

import java.util.List;

import org.mapstruct.Mapper;

import br.com.tlf.core.domain.vo.terms.TermsCatalogVO;
import br.com.tlf.infrastructure.persistence.postgresql.entity.TermsCatalogJpaEntity;

@Mapper(componentModel = "spring")
public interface TermsCatalogRepositoryMapper {

    List<TermsCatalogVO> toVO(List<TermsCatalogJpaEntity> entities);
}
