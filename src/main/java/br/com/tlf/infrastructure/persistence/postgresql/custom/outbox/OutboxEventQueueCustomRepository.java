package br.com.tlf.infrastructure.persistence.postgresql.custom.outbox;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import br.com.tlf.core.domain.vo.OutBoxEventQueueVO;
import br.com.tlf.core.port.out.outbox.OutboxEventQueueRepository;
import br.com.tlf.infrastructure.persistence.postgresql.jpa.OutboxEventQueueJpaRepository;
import br.com.tlf.infrastructure.persistence.postgresql.mapper.OutboxEventQueueRepositoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Repository("OutboxEventQueueCustomRepository")
@Primary
public class OutboxEventQueueCustomRepository implements OutboxEventQueueRepository {

    private final OutboxEventQueueJpaRepository outboxEventQueueJpaRepository;
    private final OutboxEventQueueRepositoryMapper outboxEventQueueRepositoryMapper;

    @Override
    public void save(OutBoxEventQueueVO event) {
        log.info("Persisting outbox event for aggregateId: {}", event.getAggregateId());

        outboxEventQueueJpaRepository.save(outboxEventQueueRepositoryMapper.toEntity(event));
    }
}
