package br.com.tlf.core.port.out.eventhub.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class EventHubRequestDTO {

    private Object event;
    private String eventType;

}
