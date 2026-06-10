package br.com.tlf.core.port.out.eventhub;

import br.com.tlf.core.port.out.eventhub.dto.request.EventHubRequestDTO;

public interface EventHubPort {
    void sendEvent(EventHubRequestDTO request);
}
