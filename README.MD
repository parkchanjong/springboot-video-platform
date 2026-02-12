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

