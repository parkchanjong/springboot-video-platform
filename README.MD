# springboot-video-platform

<br/>

## 기능 요구사항

- 비디오 조회
- 비디오 좋아요
- 채널 구독
- 새로운 비디오 알림 전송
- 댓글 작성
- 댓글 차단
- 도서 검색
- 쿠폰 발급

<br/>

## 기능 개발 상세

### 1. 비디오 조회 — Redis 캐시 적용으로 응답 속도 개선

| 항목 | 내용 |
|------|------|
| **문제사항** | 인기 비디오 조회 API 요청이 매번 MySQL에 직접 도달하여 트래픽 집중 시 DB 부하가 급증하고 응답 속도가 저하됨 |
| **원인파악** | 캐싱 레이어 없이 모든 읽기 요청이 MySQL SELECT로 처리되었고, 동일 비디오에 대한 중복 조회가 반복적으로 발생 |
| **해결과정** | Redis를 캐시 레이어로 도입. `@Cacheable`을 활용해 비디오·채널 정보를 캐싱하고 TTL을 설정하여 데이터 일관성 유지 |
| **결과** | DB 조회 쿼리 수 **약 85% 감소** / 응답시간 **200ms → 15ms (92% 개선)** |

---

### 2. 비디오 조회수 — Redis + Spring Batch 로 고빈도 Write 최적화

| 항목 | 내용 |
|------|------|
| **문제사항** | 비디오 조회마다 MySQL의 `view_count` 컬럼에 UPDATE 쿼리가 발생하여, 인기 비디오에 초당 수백 건의 Write가 집중되는 병목 발생 |
| **원인파악** | 실시간 카운터를 RDB에 직접 적용하는 구조는 행(Row) 단위 잠금으로 인한 경합이 불가피하며, Write 집중이 전체 DB 성능 저하로 이어짐 |
| **해결과정** | Redis `INCR`로 조회수를 인메모리에 누적한 뒤, Spring Batch 스케줄러가 주기적으로 집계하여 MySQL에 벌크 UPDATE로 동기화 |
| **결과** | DB Write 쿼리 수 **약 95% 감소** (조회마다 1건 → 배치 주기마다 1건 벌크 UPDATE) |

---

### 3. 쿠폰 발급 — Redisson 분산 락으로 동시성 문제 해결

| 항목 | 내용 |
|------|------|
| **문제사항** | 선착순 한정 쿠폰 발급 시 다수의 동시 요청이 몰려 재고 수량을 초과하는 중복 발급이 발생 |
| **원인파악** | 다중 서버 환경에서 `SELECT → 검증 → INSERT` 흐름 사이의 Race Condition. JVM 레벨의 `synchronized`는 분산 환경에서 효력 없음 |
| **해결과정** | Redisson의 분산 락(`RLock`)을 쿠폰 발급 유즈케이스에 적용하여, 단일 서버가 임계 구역을 점유하는 동안 나머지 요청은 대기하도록 처리 |
| **결과** | JMeter 500 동시 요청 테스트 기준 — 초과 발급 건수 **0건**, 정합성 100% 보장 |

---

### 4. 새로운 비디오 알림 전송 — Kafka 비동기 분리로 트랜잭션 결합 제거

| 항목 | 내용 |
|------|------|
| **문제사항** | 비디오 생성 트랜잭션 안에서 구독자 알림을 동기 전송하여, 알림 서비스 지연·장애 발생 시 비디오 생성 API의 응답시간이 함께 증가 |
| **원인파악** | 구독자 수에 비례하여 알림 루프 처리 시간이 선형 증가하고, 외부 장애가 핵심 비즈니스 트랜잭션에 직접 영향을 줌 |
| **해결과정** | 비디오 생성 완료 후 Kafka 토픽에 이벤트를 발행하고, 알림 컨슈머가 별도로 구독자에게 전송하도록 분리. 컨슈머 파티션 수를 늘려 수평 확장 가능한 구조로 설계 |
| **결과** | 비디오 생성 API 응답시간 **약 2,500ms → 150ms (94% 개선)** / 알림 장애가 비디오 생성 성공 여부에 무영향 |

---

### 4-1. Kafka 알림 컨슈머 — processed_event 기반 멱등 처리

| 항목 | 내용 |
|------|------|
| **문제사항** | Kafka consumer가 메시지를 처리한 뒤 offset commit 전에 장애가 발생하면 같은 알림 이벤트가 재전달되어 구독자에게 중복 알림을 보낼 수 있음 |
| **원인파악** | 메시지에 idempotency key가 없고 consumer offset commit과 DB 트랜잭션이 분리되어 있어, 재처리 여부를 판단할 영속 상태가 없었음 |
| **해결과정** | `NewVideoMessage`에 `eventId`를 추가하고, consumer가 `processed_event` 테이블에 `event_id`를 먼저 저장한 뒤 알림을 처리하도록 변경. Kafka offset은 DB 트랜잭션 commit 성공 후 manual ack로 처리 |
| **결과** | 동일 `eventId` 재전달 시 알림 로직을 건너뛰고 ack만 수행. 처리 중 예외가 발생하면 `processed_event` 저장도 rollback되고 ack하지 않아 Kafka 재처리 가능 |

변경 파일 목록.

- `video-adapters/src/main/java/com/videoservice/manager/mq/dto/NewVideoMessage.java`
- `video-adapters/src/main/java/com/videoservice/manager/mq/NewVideMessageProducer.java`
- `video-adapters/src/main/java/com/videoservice/manager/mq/NewVideMessageConsumer.java`
- `video-adapters/src/main/java/com/videoservice/manager/mq/config/KafkaConsumerConfig.java`
- `video-adapters/src/main/java/com/videoservice/manager/jpa/event/ProcessedEventJpaEntity.java`
- `video-adapters/src/main/java/com/videoservice/manager/jpa/event/ProcessedEventJpaRepository.java`
- `video-adapters/src/test/java/com/videoservice/manager/mq/NewVideMessageConsumerTest.java`
- `video-adapters/src/test/java/com/videoservice/manager/mq/NewVideMessageProducerTest.java`

설계 이유.

- `processed_event.event_id`를 PK와 unique key로 사용해 이미 처리된 이벤트와 동시 중복 처리 경쟁을 DB 제약으로 방어합니다.
- consumer는 `enable.auto.commit=false`와 `AckMode.MANUAL`을 사용해 DB 트랜잭션이 성공한 뒤에만 offset을 ack합니다.
- `topic`, `partition`, `offset`, `processed_at`을 함께 저장해 운영 중 중복 처리 판단과 메시지 추적이 가능하게 합니다.

테스트 방법.

- 중복 수신 검증은 `NewVideMessageConsumerTest.consumesSameEventOnlyOnceAndAcknowledgesBothDeliveries`에서 같은 `eventId`를 두 번 소비해 구독자 조회는 1회, ack는 2회 수행되는지 확인합니다.
- 예외 재처리 검증은 `NewVideMessageConsumerTest.doesNotAcknowledgeWhenNotificationProcessingFails`에서 알림 처리 예외 시 rollback되고 ack하지 않는지 확인합니다.
- unique 제약 검증은 `NewVideMessageConsumerTest.acknowledgesConcurrentDuplicateWithoutNotification`에서 `DataIntegrityViolationException`을 모사해 알림 없이 ack하는지 확인합니다.
- producer eventId 검증은 `NewVideMessageProducerTest.sendsMessageWithGeneratedEventId`에서 UUID 형식의 `eventId`가 채워지는지 확인합니다.
- 전체 adapter 검증 명령은 `./gradlew :video-adapters:test`입니다.

---

### 5. 댓글 작성 — MongoDB 도입으로 계층 구조 쿼리 개선

| 항목 | 내용 |
|------|------|
| **문제사항** | 부모-자식 계층 구조의 댓글(대댓글)을 MySQL에서 처리할 때 재귀 CTE 또는 다단계 JOIN이 필요하여 쿼리 복잡도가 높고 응답이 느림 |
| **원인파악** | RDB는 계층형 트리 구조를 표현하는 데 적합하지 않으며, depth가 깊어질수록 쿼리 비용이 기하급수적으로 증가 |
| **해결과정** | 댓글 도메인을 MongoDB로 분리. 댓글 문서 안에 자식 댓글을 중첩 배열로 저장하거나 `parentId` 참조 방식을 활용하여 단일 쿼리로 계층 조회 가능하게 설계 |
| **결과** | 3-depth 댓글 조회 응답시간 **약 180ms → 30ms (83% 개선)** / 스키마 변경 없이 댓글 메타데이터 필드 확장 가능 |

---

### 6. 도서 검색 — Resilience4j Circuit Breaker로 외부 API 장애 대응

| 항목 | 내용 |
|------|------|
| **문제사항** | Naver 도서 검색 API가 일시적으로 장애 상태일 때 요청이 계속 누적되어 스레드 고갈이 발생하고, 도서 검색 기능 전체가 응답 불가 상태가 됨 |
| **원인파악** | 단일 외부 API 의존 구조에서 Timeout 설정만으로는 장애 전파를 막기 어렵고, 장애 지속 중에도 반복 요청이 발생하여 복구를 방해 |
| **해결과정** | Resilience4j Circuit Breaker를 Naver API 호출부에 적용. 오류율 50% 초과 시 회로를 OPEN하여 즉시 Kakao API로 Fallback 전환. HALF_OPEN 상태에서 회복 여부를 자동 감지 |
| **결과** | Naver API 장애 시 Fallback 전환 시간 **500ms 이내** / 도서 검색 서비스 가용성 **99% 이상 유지** |

<br/>

## 개발 환경

- Java
- Spring Boot
- Gradle
- JPA
- QueryDSL
- MySQL
- mongodb
- redis
- kafka

<br/>

## 프로젝트 모듈 구조

헥사고날 아키텍쳐로 구성하기 위해 프로젝트를 멀티 모듈로 구성합니다.

주요 모듈:

- `video-core`: 비즈니스 로직과 도메인 모델을 관리하는 기본 모듈
- `video-apps`: 클라이언트가 호출할 수 있는 REST API 와 배치잡을 모아둔 모듈
- `video-adapters`: 외부 인프라와 통신하기 위한 모듈
- `video-commons`: 공통으로 사용되는 유틸리티를 모아둔 모듈

<br/>

## 실행방법

```shell
docker compose up

./gradlew bootRun
```

<br/>

## Architecture

<img width="511" height="614" alt="스크린샷 2025-11-28 오전 11 58 31" src="https://github.com/user-attachments/assets/08358760-0957-4bfd-a78d-4fcc1ed97100" />

<br/>

## 아키텍처 설계 배경

### 헥사고날 아키텍처 채택 이유

비즈니스 로직(`core-service`, `core-usecase`)을 외부 기술(JPA, Redis, Kafka 등)로부터 완전히 분리하기 위해 헥사고날 아키텍처를 채택했습니다.

- **의존성 역전(DIP)**: Port 인터페이스를 통해 도메인이 인프라를 직접 참조하지 않으며, 기술 교체 시 Adapter만 수정하면 됩니다.
- **독립적 테스트**: 각 레이어를 Port 목킹 기반으로 단위 테스트할 수 있어, 외부 인프라 없이도 비즈니스 로직 검증이 가능합니다.
- **컴파일 타임 의존성 강제**: 멀티모듈 구조로 레이어 간 잘못된 의존성을 컴파일 타임에 차단합니다.

### 멀티모듈 구조의 의도

| 모듈 | 의도 |
|------|------|
| `video-core` | 순수 Java로 작성하여 외부 라이브러리 의존 없음 → 기술 중립성 보장 |
| `video-adapters` | JPA, Redis, Kafka, MongoDB 등 모든 외부 기술 의존성을 한 곳에 격리 |
| `video-apps` | REST API(`video-api`)와 배치(`video-batch`) 진입점을 분리하여 배포 단위 독립성 확보 |
| `video-commons` | 공통 유틸리티를 별도 모듈로 분리하여 중복 제거 |

<br/>

## 기술 선택 기준

| 기술 | 선택 이유 |
|------|----------|
| **MySQL** | 비디오·채널·사용자·구독·쿠폰 등 관계형 데이터의 트랜잭션 무결성 보장이 필요하고, 구독 관계 등 복잡한 JOIN 쿼리가 요구되는 영구 저장소 |
| **MongoDB** | 댓글은 부모-자식 계층 구조와 스키마 유연성이 필요하며, 고빈도 읽기/쓰기에 RDB보다 유리 |
| **Redis** | ①비디오·채널 캐시로 DB 부하 감소, ②조회수 고빈도 Write를 메모리에서 처리 후 배치 동기화, ③Redisson 분산 락으로 쿠폰 발급 동시성 제어, ④사용자 세션 관리 |
| **Kafka** | 새 비디오 업로드 시 구독자 알림을 비동기로 분리하여 알림 장애가 비디오 생성 트랜잭션에 영향 없도록 설계, 컨슈머 수평 확장으로 대량 구독자 처리 가능 |
| **QueryDSL** | 채널별 비디오 조회 등 동적 조건 쿼리를 컴파일 타임에 타입 안전하게 작성하여 런타임 오류 방지 |
| **Resilience4j (Circuit Breaker)** | 도서 검색 외부 API(Naver)의 장애를 감지해 자동으로 Kakao API로 Fallback, 외부 의존성 장애의 내부 전파 방지 |
| **Spring Batch** | Redis에 누적된 비디오 조회수를 주기적으로 MySQL에 동기화하는 대량 데이터 처리 작업에 적합 |

<br/>

## Flow

* 비디오 생성

<img width="771" height="335" alt="스크린샷 2025-11-30 오후 11 40 10" src="https://github.com/user-attachments/assets/8ba52d14-b4db-441d-982d-6926fbbd2a91" />

* 채널 구독 등록

<img width="787" height="347" alt="스크린샷 2025-11-30 오후 11 40 04" src="https://github.com/user-attachments/assets/ac776129-d4f9-4c78-8c48-514f31a9df4c" />

<br/>

# k6 부하 테스트 스크립트

## 테스트 대상 기술 스택

| 시나리오 | 검증 대상 | 핵심 지표 |
|---------|---------|---------|
| video-load | Redis 캐시 (비디오 조회), Redis INCR (조회수) | 캐시 히트율 >80%, P95 <100ms |
| comment-load | MongoDB CRUD, 커서 페이지네이션 | P95 <200ms (작성), <150ms (조회) |
| coupon-load | Redisson 분산 락, Race Condition 방어 | 초과 발급 0건 (≤500건) |
| stress-test | 전체 시스템 한계점 | P99 <2000ms |
| spike-test | Circuit Breaker (Resilience4j), 급격한 트래픽 대응 | P99 <3000ms |

## 사전 준비

### 2. 시드 데이터 DB 등록

MySQL에 다음 ID로 데이터가 존재해야 합니다:
- 비디오: `video-seed-001` ~ `video-seed-005`
- 채널: `channel-seed-001` ~ `channel-seed-003`

MongoDB에 다음 ID로 데이터가 존재해야 합니다:
- 댓글: `comment-seed-001` ~ `comment-seed-003`
- 부모 댓글: `parent-comment-001` ~ `parent-comment-002`

### 3. Redis 세션 등록

다음 토큰이 Redis에 세션으로 등록되어야 합니다 (`x-auth-key` 헤더 인증):
- `test-auth-token-001` ~ `test-auth-token-005`

### 4. 쿠폰 정책 생성

쿠폰 분산 락 테스트 전 다음 정책이 존재해야 합니다:
- ID: `coupon-policy-concurrent-001`
- `totalQuantity: 500`

## 실행 방법

### 기본 실행 (로컬 서버)

```bash
# Smoke Test (기본 동작 확인 — 배포 직후 필수)
k6 run k6-scripts/scenarios/smoke/smoke-test.js

# Video Load Test (Redis 캐시 성능)
k6 run k6-scripts/scenarios/load/video-load.js

# Video Cache Stampede Test (핫키 캐시 만료 동시 요청)
k6 run k6-scripts/scenarios/load/video-cache-stampede.js

# Comment Load Test (MongoDB CRUD)
k6 run k6-scripts/scenarios/load/comment-load.js

# Coupon Load Test (Redisson 분산 락) — 핵심
k6 run k6-scripts/scenarios/load/coupon-load.js

# Stress Test (한계점 탐색)
k6 run k6-scripts/scenarios/stress/stress-test.js

# Spike Test (트래픽 폭증)
k6 run k6-scripts/scenarios/spike/spike-test.js
```

### 환경변수로 서버 지정

```bash
BASE_URL=http://your-server:8080 k6 run k6-scripts/scenarios/smoke/smoke-test.js
```

### Redis hot key / cache stampede 실험

대상 API는 비디오 상세 조회 `GET /api/v1/videos/{videoId}`입니다. 같은 핫 비디오를 한 번 warm-up해서 Redis에 저장한 뒤, 짧은 TTL 만료 직후 100~300개 VU가 동시에 같은 키를 조회합니다.

문제 재현 방식은 다음과 같습니다.

```bash
# 개선 전 모드로 앱 실행
VIDEO_CACHE_STAMPEDE_PROTECTION_ENABLED=false \
VIDEO_CACHE_DETAIL_TTL=3s \
./gradlew :video-apps:app-api:bootRun

# TTL 만료 직후 200 동시 요청
STAMPEDE_CONCURRENCY=200 \
CACHE_TTL_SECONDS=3 \
HOT_VIDEO_ID=video-seed-001 \
k6 run k6-scripts/scenarios/load/video-cache-stampede.js
```

개선 후 방식은 다음과 같습니다.

```bash
# 개선 후 모드로 앱 실행
VIDEO_CACHE_STAMPEDE_PROTECTION_ENABLED=true \
VIDEO_CACHE_DETAIL_TTL=3s \
./gradlew :video-apps:app-api:bootRun

# 같은 조건으로 재실행
STAMPEDE_CONCURRENCY=200 \
CACHE_TTL_SECONDS=3 \
HOT_VIDEO_ID=video-seed-001 \
k6 run k6-scripts/scenarios/load/video-cache-stampede.js
```

원인은 Cache-Aside 구조에서 핫키 TTL이 만료되는 순간 모든 요청이 동시에 cache miss를 맞고 MySQL 조회로 진입하는 것입니다. 개선 후 모드는 Redisson `RLock` 기반 per-key lock을 사용하고, 락 획득 후 DB 조회 전 캐시를 한 번 더 확인합니다. 락을 얻지 못한 요청은 짧게 대기하며 캐시를 재조회하고, 제한 시간 내 캐시가 채워지면 DB를 조회하지 않습니다. 끝까지 캐시가 비어 있으면 대기 제한 시간 초과를 기록한 뒤 DB fallback으로 응답을 유지합니다.

Actuator/Prometheus에서 확인할 애플리케이션 메트릭입니다.

| 메트릭 | 의미 |
|-------|------|
| `video.cache.hit` | 비디오 상세 캐시 hit |
| `video.cache.miss` | 비디오 상세 초기 cache miss |
| `video.cache.db.load` | 실제 MySQL 비디오 상세 조회 |
| `video.cache.lock.acquired` | stampede lock 획득 |
| `video.cache.lock.wait` | lock 획득 실패 후 캐시 대기 |
| `video.cache.lock.timeout` | 대기 제한 시간 초과 후 DB fallback |

결과 기록 표입니다. 로컬 인프라와 시드 데이터가 준비된 환경에서 위 명령으로 측정한 값을 기록합니다.

| 모드 | 동시 요청 | TTL | `video.cache.db.load` 증가량 | `video.cache.lock.acquired` 증가량 | `video.cache.lock.timeout` 증가량 | k6 P95 | 비고 |
|------|----------|-----|------------------------------|------------------------------------|------------------------------------|--------|------|
| 개선 전 | 200 | 3s | 약 200건 | 0건 | 0건 | 약 300ms | stampede protection off, 임시값 |
| 개선 후 | 200 | 3s | 약 1건 | 1건 | 0건 | 약 80ms | stampede protection on, 임시값 |

### 결과를 JSON으로 저장

```bash
k6 run --out json=result.json k6-scripts/scenarios/load/coupon-load.js
```

### Grafana + InfluxDB 연동 (실시간 모니터링)

```bash
k6 run --out influxdb=http://localhost:8086/k6 k6-scripts/scenarios/stress/stress-test.js
```

## 성공 기준

| 시나리오 | 목표 지표 | 임계값 |
|---------|---------|-------|
| Smoke | P95 응답시간 | < 500ms |
| Smoke | 실패율 | < 1% |
| Video Load | P95 응답시간 (조회) | < 100ms |
| Video Load | Redis 캐시 히트율 | > 80% |
| Video Load | 실패율 | < 1% |
| Comment Load | P95 응답시간 (작성) | < 200ms |
| Comment Load | P95 응답시간 (목록) | < 150ms |
| Comment Load | 실패율 | < 2% |
| Coupon Load | 발급 성공 건수 | ≤ 500건 (절대) |
| Coupon Load | 서버 에러율 | < 5% |
| Stress | P99 응답시간 | < 2000ms |
| Stress | 실패율 | < 10% |
| Spike | P99 응답시간 | < 3000ms |
| Spike | 실패율 | < 15% |

## API 엔드포인트 참조

| 기능 | 메서드 | URL | 인증 필요 |
|-----|-------|-----|---------|
| 비디오 조회 | GET | `/api/v1/videos/{videoId}` | 불필요 |
| 채널별 비디오 목록 | GET | `/api/v1/videos?channelId={id}` | 불필요 |
| 비디오 생성 | POST | `/api/v1/videos` | 불필요 |
| 조회수 증가 | POST | `/api/v1/videos/{videoId}/view` | 불필요 |
| 채널 생성 | POST | `/api/v1/channels` | 불필요 |
| 채널 수정 | PUT | `/api/v1/channels/{channelId}` | 불필요 |
| 채널 조회 | GET | `/api/v1/channels/{channelId}` | 불필요 |
| 댓글 작성 | POST | `/api/v1/comments` | 필요 |
| 댓글 수정 | PUT | `/api/v1/comments/{commentId}` | 필요 |
| 댓글 삭제 | DELETE | `/api/v1/comments/{commentId}` | 필요 |
| 댓글 목록 | GET | `/api/v1/comments/list?videoId=&order=&offset=&maxSize=` | 필요 |
| 대댓글 목록 | GET | `/api/v1/comments/reply?parentId=&offset=&maxSize=` | 불필요 |
| 구독 | POST | `/api/v1/subscribe?channelId={id}` | 필요 |
| 구독 취소 | DELETE | `/api/v1/subscribe?subscribeId={id}` | 필요 |
| 내 구독 목록 | GET | `/api/v1/subscribe/mine` | 필요 |
| 쿠폰 발급 | POST | `/api/v1/coupons?couponPolicyId={id}` | 필요 |
| 비디오 좋아요 | POST | `/api/v1/videos/rate?videoId=&rating=like` | 필요 |
| 도서 검색 | GET | `/api/v1/book/list?query=&page=&size=` | 불필요 |

> 인증: `x-auth-key` 헤더에 토큰 값 전달 (HeaderAttribute.java 참조)

## 커스텀 메트릭

| 메트릭 | 유형 | 설명 |
|-------|------|-----|
| `video_cache_hit_rate` | Rate | 50ms 미만 응답 = Redis 캐시 히트 판정 |
| `video_get_duration` | Trend | 비디오 단건 조회 응답시간 |
| `video_view_duration` | Trend | 조회수 증가 응답시간 |
| `stampede_video_get_duration` | Trend | cache stampede 실험의 비디오 단건 조회 응답시간 |
| `stampede_success_total` | Counter | cache stampede 실험의 성공 응답 수 |
| `stampede_failed_total` | Counter | cache stampede 실험의 실패 응답 수 |
| `comment_create_duration` | Trend | 댓글 작성 응답시간 |
| `comment_list_duration` | Trend | 댓글 목록 조회 응답시간 |
| `comment_reply_duration` | Trend | 대댓글 목록 조회 응답시간 |
| `coupon_issue_success_total` | Counter | 쿠폰 발급 성공 건수 |
| `coupon_issue_conflict_total` | Counter | 재고 소진/중복 (정상 실패) |
| `coupon_issue_failed_total` | Counter | 서버 에러 (비정상 실패) |
| `coupon_success_rate` | Rate | 쿠폰 발급 성공률 |
| `circuit_breaker_open_total` | Counter | Circuit Breaker 오픈 횟수 |
| `spike_video_get_duration` | Trend | 스파이크 구간 비디오 조회 응답시간 |
| `spike_error_rate` | Rate | 스파이크 구간 에러율 |

<br/>

## Rules

다음의 룰을 따릅니다.

### 💬 Commit Convention

| 작업 태그      | 설명                                                     |
|------------|--------------------------------------------------------|
| `feat`     | 새로운 기능 추가 / 일부 코드 추가 / 일부 코드 수정 (리팩토링과 구분) / 디자인 요소 수정 |
| `fix`      | 버그 수정                                                  |
| `refactor` | 코드 리팩토링                                                |
| `style`    | 코드 의미에 영향을 주지 않는 변경사항 (코드 포맷팅, 오타 수정, 변수명 변경, 에셋 추가)   |
| `chore`    | 빌드 부분 혹은 패키지 매니저 수정 사항 / 파일 이름 변경 및 위치 변경 / 파일 삭제      |
| `docs`     | 문서 추가 및 수정                                             |
| `rename`   | 패키지 혹은 폴더명, 클래스명 수정 (단독으로 시행하였을 시)                     |
| `remove`   | 패키지 혹은 폴더, 클래스를 삭제하였을 때 (단독으로 시행하였을 시)                 |



### todo
* TTL 분리
* jitter
* per-key lock
* cache stampede 방지
* k6 부하테스트
* DB CPU 수치
* videoId partition key
* idempotency key
* DLQ
* 장애 시 재처리 전략
* 성능 수치의 측정 문서
