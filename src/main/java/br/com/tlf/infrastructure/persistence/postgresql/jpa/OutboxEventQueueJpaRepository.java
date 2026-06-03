package br.com.tlf.infrastructure.persistence.postgresql.jpa;

import br.com.tlf.infrastructure.persistence.postgresql.entity.OutboxEventQueueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxEventQueueJpaRepository extends JpaRepository<OutboxEventQueueEntity, UUID> {
}
