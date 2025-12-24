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

