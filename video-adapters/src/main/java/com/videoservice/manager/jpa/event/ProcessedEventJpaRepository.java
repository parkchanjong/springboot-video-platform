// 처리 완료된 Kafka 이벤트 조회와 저장을 담당하는 리포지토리
package com.videoservice.manager.jpa.event;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventJpaEntity, String> {
}
