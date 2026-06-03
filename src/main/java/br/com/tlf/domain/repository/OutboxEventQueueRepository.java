package br.com.tlf.domain.repository;

import br.com.tlf.domain.entity.OutboxEventQueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxEventQueueRepository extends JpaRepository<OutboxEventQueueEntity, UUID> {
}
