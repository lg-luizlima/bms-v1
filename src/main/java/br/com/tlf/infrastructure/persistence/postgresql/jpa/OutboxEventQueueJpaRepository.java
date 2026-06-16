package br.com.tlf.infrastructure.persistence.postgresql.jpa;

import br.com.tlf.infrastructure.persistence.postgresql.entity.OutboxEventQueueJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxEventQueueJpaRepository extends JpaRepository<OutboxEventQueueJpaEntity, UUID> {
}
