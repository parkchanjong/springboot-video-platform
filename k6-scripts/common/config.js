// =====================================================================
// 공통 설정: BASE_URL, 시드 데이터 ID 풀, 임계값(thresholds) 정의
// =====================================================================

/** 서버 기본 URL (환경변수로 오버라이드 가능) */
export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// =====================================================================
// 시드 데이터 ID 풀
// 테스트 실행 전 DB/Redis에 해당 데이터가 존재해야 함
// =====================================================================
export const TEST_DATA = {
  // Redis 세션에 등록된 인증 토큰 (x-auth-key 헤더 값)
  AUTH_TOKENS: [
    'test-auth-token-001',
    'test-auth-token-002',
    'test-auth-token-003',
    'test-auth-token-004',
    'test-auth-token-005',
  ],

  // MySQL에 등록된 비디오 ID (Redis 캐시 테스트용)
  VIDEO_IDS: [
    'video-seed-001',
    'video-seed-002',
    'video-seed-003',
    'video-seed-004',
    'video-seed-005',
  ],

  // MySQL에 등록된 채널 ID
  CHANNEL_IDS: [
    'channel-seed-001',
    'channel-seed-002',
    'channel-seed-003',
  ],

  // MongoDB에 등록된 댓글 ID
  COMMENT_IDS: [
    'comment-seed-001',
    'comment-seed-002',
    'comment-seed-003',
  ],

  // MongoDB에 등록된 부모 댓글 ID (대댓글 테스트용)
  PARENT_COMMENT_IDS: [
    'parent-comment-001',
    'parent-comment-002',
  ],

  // Redisson 분산 락 동시성 테스트용 쿠폰 정책 ID
  // totalQuantity=500으로 사전 생성 필요
  COUPON_POLICY_ID: 'coupon-policy-concurrent-001',

  // MySQL에 등록된 구독 ID
  SUBSCRIBE_IDS: [
    'subscribe-seed-001',
    'subscribe-seed-002',
  ],
};

// =====================================================================
// 인증 헤더 생성 함수
// HeaderAttribute.java: X_AUTH_KEY = "x-auth-key"
// =====================================================================

/**
 * x-auth-key 헤더를 포함한 공통 요청 헤더 반환
 * @param {string} token - AUTH_TOKENS 중 하나
 * @returns {Object} HTTP 헤더 객체
 */
export function makeAuthHeaders(token) {
  return {
    'Content-Type': 'application/json',
    'x-auth-key': token,
  };
}

/**
 * 인증 없이 사용 가능한 기본 헤더 반환
 * @returns {Object} HTTP 헤더 객체
 */
export function makeHeaders() {
  return {
    'Content-Type': 'application/json',
  };
}

// =====================================================================
// 시나리오별 임계값(thresholds) 상수
// =====================================================================

/** Smoke Test: 기본 동작 확인 */
export const SMOKE_THRESHOLDS = {
  http_req_failed: [{ threshold: 'rate<0.01', abortOnFail: false }],
  http_req_duration: [{ threshold: 'p(95)<500', abortOnFail: false }],
};

/** Video Load: Redis 캐시 효과 + 조회수 동시성 */
export const VIDEO_LOAD_THRESHOLDS = {
  http_req_failed: [{ threshold: 'rate<0.01', abortOnFail: false }],
  // 비디오 단건 조회 P95 < 100ms (캐시 히트 시 매우 빠름)
  'http_req_duration{endpoint:video_get}': [{ threshold: 'p(95)<100', abortOnFail: false }],
  // 캐시 히트율 > 80% (50ms 미만 응답을 캐시 히트로 판정)
  video_cache_hit_rate: [{ threshold: 'rate>0.80', abortOnFail: false }],
};

/** Comment Load: MongoDB CRUD 성능 */
export const COMMENT_LOAD_THRESHOLDS = {
  http_req_failed: [{ threshold: 'rate<0.02', abortOnFail: false }],
  // 댓글 작성 P95 < 200ms
  'http_req_duration{endpoint:comment_create}': [{ threshold: 'p(95)<200', abortOnFail: false }],
  // 댓글 목록 조회 P95 < 150ms
  'http_req_duration{endpoint:comment_list}': [{ threshold: 'p(95)<150', abortOnFail: false }],
  // 대댓글 조회 P95 < 150ms
  'http_req_duration{endpoint:comment_reply}': [{ threshold: 'p(95)<150', abortOnFail: false }],
};

/** Coupon Load: Redisson 분산 락 동시성 — 핵심 임계값 */
export const COUPON_LOAD_THRESHOLDS = {
  // 서버 에러(5xx)는 5% 미만이어야 함 (재고 소진 409/400은 정상)
  http_req_failed: [{ threshold: 'rate<0.05', abortOnFail: false }],
  // 발급 성공 건수는 쿠폰 재고(500개)를 초과해서는 안 됨
  coupon_issue_success_total: [{ threshold: 'count<=500', abortOnFail: false }],
};

/** Stress Test: 시스템 한계점 탐색 */
export const STRESS_THRESHOLDS = {
  http_req_failed: [{ threshold: 'rate<0.10', abortOnFail: false }],
  http_req_duration: [{ threshold: 'p(99)<2000', abortOnFail: false }],
};

/** Spike Test: 급격한 트래픽 폭증 */
export const SPIKE_THRESHOLDS = {
  http_req_failed: [{ threshold: 'rate<0.15', abortOnFail: false }],
  http_req_duration: [{ threshold: 'p(99)<3000', abortOnFail: false }],
};
