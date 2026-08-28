package br.com.tlf.core.port.out.outbox;

import br.com.tlf.core.domain.consent.ConsentRegisteredEvent;


public interface ConsentEventOutbox {

    void publish(String aggregateId, ConsentRegisteredEvent event);
}
