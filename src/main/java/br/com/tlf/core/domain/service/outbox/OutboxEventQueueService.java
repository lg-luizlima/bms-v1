package br.com.tlf.core.domain.service.outbox;

import br.com.tlf.core.domain.vo.OutBoxEventQueueVO;

public interface OutboxEventQueueService {

    void save(OutBoxEventQueueVO entity);
}
