// =====================================================================
// 공통 유틸리티 함수
// =====================================================================
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { TEST_DATA } from './config.js';

// =====================================================================
// 무작위 데이터 유틸
// =====================================================================

/**
 * 배열에서 무작위 요소 하나를 반환
 * @param {Array} arr
 * @returns {*}
 */
export function randomItem(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

/**
 * VU ID 기반으로 인증 토큰을 순환 배정
 * VU마다 고정된 토큰을 사용하여 세션 일관성 유지
 * @returns {string} AUTH_TOKENS 중 하나
 */
export function getTokenForVU() {
  const tokens = TEST_DATA.AUTH_TOKENS;
  // __VU는 1부터 시작하므로 0-indexed 변환
  return tokens[(__VU - 1) % tokens.length];
}

/**
 * min 이상 max 미만의 무작위 정수 반환
 * @param {number} min
 * @param {number} max
 * @returns {number}
 */
export function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min)) + min;
}

/**
 * 댓글 페이지네이션용 ISO-8601 커서 오프셋 생성
 * 현재 시각 기준 무작위 과거 시점을 반환
 * @returns {string} ISO-8601 형식 문자열
 */
export function generateCursorOffset() {
  // 최근 30일 내 무작위 시각
  const daysAgo = randomInt(0, 30);
  const hoursAgo = randomInt(0, 24);
  const date = new Date();
  date.setDate(date.getDate() - daysAgo);
  date.setHours(date.getHours() - hoursAgo);
  return date.toISOString();
}

// =====================================================================
// 테스트 데이터 생성
// =====================================================================

const VIDEO_TITLE_PREFIXES = [
  '완벽 가이드',
  '입문 강의',
  '실전 튜토리얼',
  '심화 학습',
  '핵심 정리',
  '빠르게 배우는',
  '기초부터 끝까지',
  '실무 활용',
];

const VIDEO_TOPICS = [
  'Spring Boot',
  'Redis',
  'Kafka',
  'MongoDB',
  'Docker',
  'Kubernetes',
  'Java 17',
  'MSA',
  'DDD',
  'CI/CD',
];

/**
 * 무작위 비디오 제목 생성
 * @returns {string}
 */
export function randomVideoTitle() {
  const prefix = randomItem(VIDEO_TITLE_PREFIXES);
  const topic = randomItem(VIDEO_TOPICS);
  return `${prefix}: ${topic} - ${randomInt(1, 100)}강`;
}

const COMMENT_TEXTS = [
  '정말 유익한 강의입니다. 덕분에 많이 배웠어요!',
  '설명이 너무 명확해서 이해하기 쉬웠습니다.',
  '실무에서 바로 적용할 수 있을 것 같아요.',
  '다음 강의도 기대됩니다!',
  '질문이 있는데요, 이 부분을 좀 더 자세히 설명해 주실 수 있나요?',
  '오류가 발생했는데 어떻게 해결하나요?',
  '강의 자료도 공유해 주실 수 있을까요?',
  '정말 감사합니다. 최고의 강의예요!',
  '코드 예제가 도움이 많이 됐습니다.',
  '이 방법 말고 다른 방법도 있나요?',
];

/**
 * 무작위 댓글 텍스트 생성
 * @returns {string}
 */
export function randomCommentText() {
  return randomItem(COMMENT_TEXTS);
}

// =====================================================================
// 응답 검증 유틸
// =====================================================================

/**
 * HTTP 응답 상태코드와 응답시간을 공통 검증
 * check 실패 시 콘솔에 요청/응답 정보 출력
 * @param {Object} res - k6 Response 객체
 * @param {number} status - 기대 HTTP 상태코드
 * @param {string} name - 검증 이름 (로그용)
 * @returns {boolean} 검증 통과 여부
 */
export function checkStatus(res, status, name) {
  const result = check(res, {
    [`${name}: status ${status}`]: (r) => r.status === status,
  });

  if (!result) {
    console.error(
      `[FAIL] ${name} | status=${res.status} | body=${res.body ? res.body.substring(0, 200) : 'empty'}`
    );
  }

  return result;
}

/**
 * CommandResponse에서 생성된 리소스 ID를 추출
 * @example
 *   // API 응답: { "id": "abc-123" }
 *   const id = extractId(res); // "abc-123"
 * @param {Object} res - k6 Response 객체
 * @returns {string|null} 리소스 ID 또는 파싱 실패 시 null
 */
export function extractId(res) {
  try {
    const body = JSON.parse(res.body);
    return body.id || null;
  } catch (e) {
    return null;
  }
}

// =====================================================================
// Sleep 유틸 (실제 사용 패턴 시뮬레이션)
// =====================================================================

/**
 * 실제 사용자 행동 패턴 대기 (0.5 ~ 2.0초)
 * 화면 읽기, 클릭 등 자연스러운 인터랙션 간격
 */
export function userThinkTime() {
  sleep(0.5 + Math.random() * 1.5);
}

/**
 * 연속 요청 간 최소 대기 (0 ~ 0.2초)
 * 버스트 트래픽 시나리오나 API 연쇄 호출 시 사용
 */
export function burstSleep() {
  sleep(Math.random() * 0.2);
}

// =====================================================================
// 파레토 분포 (80/20 규칙)
// 인기 콘텐츠 집중 패턴 — Redis 캐시 히트율 측정에 활용
// =====================================================================

/**
 * 파레토 분포로 비디오 ID 선택
 * 상위 3개 비디오에 80% 트래픽 집중 (핫 콘텐츠 시뮬레이션)
 * @returns {string} VIDEO_IDS 중 하나
 */
export function paretoVideoId() {
  const videos = TEST_DATA.VIDEO_IDS;
  const rand = Math.random();

  if (rand < 0.80) {
    // 80% 확률: 상위 3개 비디오 중 하나 (균등 분배)
    return videos[randomInt(0, 3)];
  } else {
    // 20% 확률: 나머지 비디오 중 하나
    return videos[randomInt(3, videos.length)];
  }
}
