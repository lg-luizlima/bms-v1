package br.com.tlf.shared.configuration.common.webclient.dto;

import java.util.Objects;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.web.reactive.function.client.ClientResponse;

import br.com.tlf.shared.configuration.common.webclient.exception.NotificationException;

@Mapper(componentModel = "spring")
public interface ExceptionErrorDetailsMapper {

    @Mapping(target = "value", source = "resp", qualifiedByName = "getStatusCode")
    NotificationException toNotificationException(ExceptionErrorDetailsDTO exceptionErrorDetailsDTO, ClientResponse resp);

    @Named("getStatusCode")
    static Integer getStatusCode(ClientResponse resp) {
        if (Objects.isNull(resp)) {
            return null;
        }
        return resp.statusCode().value();
    }
}
