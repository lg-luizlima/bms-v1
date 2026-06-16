package br.com.tlf.core.application.service.outbox;

import br.com.tlf.core.application.mapper.outboxeventqueue.OutBoxEventQueueMapper;
import br.com.tlf.infrastructure.persistence.postgresql.entity.OutboxEventQueueJpaEntity;
import br.com.tlf.infrastructure.persistence.postgresql.jpa.OutboxEventQueueJpaRepository;
import br.com.tlf.core.domain.service.outbox.OutboxEventQueueService;
import br.com.tlf.core.domain.vo.OutBoxEventQueueVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventQueueServiceImpl implements OutboxEventQueueService {

    private final OutboxEventQueueJpaRepository outboxEventQueueRepository;
    private final OutBoxEventQueueMapper outBoxEventQueueMapper;

    @Override
    @Transactional
    public void save(OutBoxEventQueueVO entity) {

        // OutboxEventQueueEntity outboxEventQueueEntity = outBoxEventQueueMapper.toOutboxEventQueueEntity(entity);

        // outboxEventQueueRepository.save(outboxEventQueueEntity);

    }
}
