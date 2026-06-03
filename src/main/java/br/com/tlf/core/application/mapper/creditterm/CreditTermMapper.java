package br.com.tlf.core.application.mapper.creditterm;

import br.com.tlf.core.domain.vo.terms.PendingTermVO;
import br.com.tlf.core.domain.vo.terms.TermsCatalogVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CreditTermMapper {

    CreditTermMapper INSTANCE = Mappers.getMapper(CreditTermMapper.class);

    @Mapping(source = "id",          target = "termId")
    List<PendingTermVO> toPendingTermVO(List<TermsCatalogVO> catalog);

}
