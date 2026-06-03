package br.com.tlf.domain.service.outbox;

import br.com.tlf.domain.entity.OutboxEventQueueEntity;

public interface OutboxEventQueueService {

    void save(OutboxEventQueueEntity entity);
}
