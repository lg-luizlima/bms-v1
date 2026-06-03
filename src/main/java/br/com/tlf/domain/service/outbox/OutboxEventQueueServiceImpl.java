package br.com.tlf.domain.service.outbox;

import br.com.tlf.domain.entity.OutboxEventQueueEntity;
import br.com.tlf.domain.repository.OutboxEventQueueRepository;
import br.com.tlf.domain.util.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static br.com.tlf.application.ApplicationConstants.CLASS_METHOD_MESSAGE_PATTERN;

@Service
@RequiredArgsConstructor
public class OutboxEventQueueServiceImpl implements OutboxEventQueueService {

    private final OutboxEventQueueRepository outboxEventQueueRepository;

    @Override
    @Transactional
    public void save(OutboxEventQueueEntity entity) {
        LogUtils.log(CLASS_METHOD_MESSAGE_PATTERN, this.getClass().getSimpleName(), "save");
        outboxEventQueueRepository.save(entity);
    }
}
