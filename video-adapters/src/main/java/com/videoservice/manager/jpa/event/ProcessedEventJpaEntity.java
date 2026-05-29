// 처리 완료된 Kafka 이벤트를 저장하는 JPA 엔티티
package com.videoservice.manager.jpa.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "processed_event",
        uniqueConstraints = @UniqueConstraint(name = "uk_processed_event_event_id", columnNames = "event_id")
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ProcessedEventJpaEntity {
    @Id
    @Column(name = "event_id")
    private String eventId;

    private String topic;

    @Column(name = "`partition`")
    private Integer partition;

    @Column(name = "`offset`")
    private Long offset;

    private LocalDateTime processedAt;

    public static ProcessedEventJpaEntity of(String eventId, String topic, Integer partition, Long offset) {
        return new ProcessedEventJpaEntity(eventId, topic, partition, offset, LocalDateTime.now());
    }
}
