package br.com.tlf.infrastructure.persistence.postgresql.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import br.com.tlf.core.domain.consent.CustomerConsent;
import br.com.tlf.core.domain.consent.Signature;
import br.com.tlf.infrastructure.persistence.postgresql.entity.CustomerConsentJpaEntity;
import br.com.tlf.shared.util.JsonSerializer;

@Mapper(componentModel = "spring")
public abstract class CustomerConsentRepositoryMapper {

    @Autowired
    protected JsonSerializer jsonSerializer;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cpf", source = "customerId")
    @Mapping(target = "auditDetails", source = "signature")
    public abstract CustomerConsentJpaEntity toEntity(CustomerConsent consent);

    @Mapping(target = "customerId", source = "cpf")
    @Mapping(target = "signature", source = "auditDetails")
    public abstract CustomerConsent toDomain(CustomerConsentJpaEntity entity);

    protected String signatureToJson(Signature signature) {
        return jsonSerializer.toJson(signature);
    }

    protected Signature jsonToSignature(String auditDetails) {
        return jsonSerializer.fromJson(auditDetails, Signature.class);
    }
}
