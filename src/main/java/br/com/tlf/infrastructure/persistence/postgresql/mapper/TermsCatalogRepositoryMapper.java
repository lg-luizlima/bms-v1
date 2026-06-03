package br.com.tlf.infrastructure.persistence.postgresql.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import br.com.tlf.infrastructure.persistence.postgresql.entity.TermsCatalogEntity;
import br.com.tlf.core.domain.vo.terms.TermsCatalogVO;

@Mapper(componentModel = "spring")
public interface TermsCatalogRepositoryMapper {
    
    TermsCatalogRepositoryMapper INSTANCE = Mappers.getMapper(TermsCatalogRepositoryMapper.class);

    List<TermsCatalogVO> toVO(List<TermsCatalogEntity> entities);
}
