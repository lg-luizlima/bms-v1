package br.com.tlf.core.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OutBoxEventQueueVO {

    private UUID eventId;
    private String aggregateType;
    private String aggregateId;
    private String topicName;
    private String payload;
    private Instant createdAt;
    private String traceContext;

}
