package br.com.tlf.core.port.out.outbox;

import br.com.tlf.core.domain.vo.OutBoxEventQueueVO;

public interface OutboxEventQueueRepository {

    void save(OutBoxEventQueueVO event);
}
