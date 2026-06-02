package br.com.tlf.configuration.common.rest.dto.v1.error;

import br.com.tlf.configuration.common.rest.exceptionhandler.error.NotificationException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.web.reactive.function.client.ClientResponse;

import java.util.Objects;

@Mapper(componentModel = "spring")
public interface ExceptionErrorDetailsMapper {

    @Mapping(target = "value", source = "resp",  qualifiedByName = "getStatusCode")
    NotificationException toNotificationException(ExceptionErrorDetailsDTO exceptionErrorDetailsDTO, ClientResponse resp);

//    @Mapping(target = "statusCode", source = "value")
//    ExceptionErrorDetailsDTO toExceptionErrorDetailsDTO(NotificationException notificationException);

    @Named("getStatusCode")
    static Integer getStatusCode(ClientResponse resp){
        if (Objects.isNull(resp)){
            return null;
        }
        return resp.statusCode().value();
    }
}
