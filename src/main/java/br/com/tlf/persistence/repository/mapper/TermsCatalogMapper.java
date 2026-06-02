package br.com.tlf.persistence.repository.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import br.com.tlf.domain.entity.TermsCatalogEntity;
import br.com.tlf.domain.vo.terms.TermsCatalogVO;

@Mapper(componentModel = "spring")
public interface TermsCatalogMapper {
    
    TermsCatalogMapper INSTANCE = Mappers.getMapper(TermsCatalogMapper.class);

    List<TermsCatalogVO> toVO(List<TermsCatalogEntity> entities);
}
