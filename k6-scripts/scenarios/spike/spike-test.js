// =====================================================================
// Spike Test: 급격한 트래픽 폭증 시뮬레이션
//
// 목적:
//   - 라이브 이벤트(예: 유명 채널 신규 영상) 업로드 순간 폭증 시뮬레이션
//   - 단일 인기 비디오 집중 조회 (핫 콘텐츠 패턴)
//   - Circuit Breaker 동작 확인 (도서 검색 외부 API)
//   - 스파이크 후 시스템 회복 속도 측정
//
// 단계:
//   10 VU → 500 VU (30초 급증) → 500 VU 유지 → 10 VU (30초 급감) → 회복 확인 → 2차 스파이크
// =====================================================================
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { BASE_URL, TEST_DATA, makeAuthHeaders, makeHeaders, SPIKE_THRESHOLDS } from '../../common/config.js';
import {
  randomItem,
  getTokenForVU,
  checkStatus,
  burstSleep,
  userThinkTime,
} from '../../common/helpers.js';

// =====================================================================
// 커스텀 메트릭
// =====================================================================

/** 스파이크 구간 응답시간 */
const spikeGetDuration = new Trend('spike_video_get_duration', true);

/** Circuit Breaker 오픈 감지 (503 응답) */
const circuitBreakerOpenTotal = new Counter('circuit_breaker_open_total');

/** 스파이크 구간 에러율 */
const spikeErrorRate = new Rate('spike_error_rate');

// =====================================================================
// 테스트 설정
// =====================================================================
export const options = {
  stages: [
    // 기준선 (안정 상태)
    { duration: '1m', target: 10 },

    // 1차 스파이크: 30초 만에 500 VU로 급증
    { duration: '30s', target: 500 },

    // 스파이크 유지 (2분)
    { duration: '2m', target: 500 },

    // 급감: 30초 만에 10 VU로 복귀
    { duration: '30s', target: 10 },

    // 회복 확인 (2분): 시스템이 정상으로 돌아오는지 측정
    { duration: '2m', target: 10 },

    // 2차 스파이크 (더 짧은 버스트)
    { duration: '30s', target: 300 },
    { duration: '1m', target: 300 },

    // 최종 쿨다운
    { duration: '30s', target: 10 },
    { duration: '1m', target: 0 },
  ],

  thresholds: {
    ...SPIKE_THRESHOLDS,
    spike_video_get_duration: [{ threshold: 'p(99)<3000', abortOnFail: false }],
    // Circuit Breaker 오픈 횟수는 임계값 없음 (관찰용)
    circuit_breaker_open_total: [{ threshold: 'count>=0', abortOnFail: false }],
  },
};

// 스파이크 집중 대상: 인기 비디오 1개 (핫 콘텐츠 시뮬레이션)
const HOT_VIDEO_ID = TEST_DATA.VIDEO_IDS[0];

// =====================================================================
// 메인 시나리오
// =====================================================================
export default function () {
  const rand = Math.random();

  if (rand < 0.70) {
    // 70%: 핫 비디오 단건 조회 (스파이크의 핵심)
    doHotVideoGet();
  } else if (rand < 0.85) {
    // 15%: 조회수 증가 (동시 INCR 폭증)
    doViewCountSpike();
  } else if (rand < 0.95) {
    // 10%: 핫 비디오 댓글 목록 (반응성 확인)
    doCommentListSpike();
  } else {
    // 5%: 도서 검색 (외부 API - Circuit Breaker 동작 확인)
    doBookSearchWithCB();
  }
}

// =====================================================================
// 워크로드 함수
// =====================================================================

/** 핫 비디오 집중 조회 (스파이크 핵심 부하) */
function doHotVideoGet() {
  const headers = makeHeaders();

  const res = http.get(
    `${BASE_URL}/api/v1/videos/${HOT_VIDEO_ID}`,
    {
      headers,
      tags: { endpoint: 'spike_hot_video_get' },
      timeout: '10s',
    }
  );

  const isSuccess = res.status === 200;
  spikeErrorRate.add(!isSuccess);
  spikeGetDuration.add(res.timings.duration);

  if (!isSuccess) {
    console.error(
      `[SPIKE] 핫 비디오 조회 실패 | VU=${__VU} | status=${res.status} | duration=${res.timings.duration}ms`
    );
  }

  burstSleep();
}

/** 조회수 폭증 (스파이크 시 Redis INCR 원자성 검증) */
function doViewCountSpike() {
  const headers = makeHeaders();

  const res = http.post(
    `${BASE_URL}/api/v1/videos/${HOT_VIDEO_ID}/view`,
    null,
    {
      headers,
      tags: { endpoint: 'spike_view_count' },
      timeout: '5s',
    }
  );

  const isSuccess = res.status === 200;
  spikeErrorRate.add(!isSuccess);

  burstSleep();
}

/** 스파이크 중 댓글 목록 조회 */
function doCommentListSpike() {
  const token = getTokenForVU();
  const authHeaders = makeAuthHeaders(token);

  const offset = new Date().toISOString();

  const res = http.get(
    `${BASE_URL}/api/v1/comments/list?videoId=${HOT_VIDEO_ID}&order=time&offset=${encodeURIComponent(offset)}&maxSize=10`,
    {
      headers: authHeaders,
      tags: { endpoint: 'spike_comment_list' },
      timeout: '10s',
    }
  );

  const isSuccess = res.status === 200;
  spikeErrorRate.add(!isSuccess);

  burstSleep();
}

/**
 * 도서 검색 (외부 API + Resilience4j Circuit Breaker)
 * 스파이크 중 외부 의존 API 호출 → CB 오픈 시 503 응답
 * 200(성공) 또는 503(CB 오픈) 모두 정상으로 처리
 */
function doBookSearchWithCB() {
  const headers = makeHeaders();

  const queries = ['spring', 'java', 'redis', 'kafka', 'docker'];
  const query = randomItem(queries);

  const res = http.get(
    `${BASE_URL}/api/v1/book/list?query=${query}&page=1&size=5`,
    {
      headers,
      tags: { endpoint: 'spike_book_search' },
      timeout: '10s',
    }
  );

  if (res.status === 503) {
    // Circuit Breaker 오픈 — 정상적인 폴백
    circuitBreakerOpenTotal.add(1);

    check(res, {
      'book_cb_open: Circuit Breaker 오픈 (정상 폴백)': (r) => r.status === 503,
    });

  } else if (res.status === 200) {
    check(res, {
      'book_search: 정상 응답': (r) => r.status === 200,
    });

  } else if (res.status >= 500) {
    // 503 외 5xx는 비정상
    spikeErrorRate.add(true);
    console.error(`[SPIKE-CB] 도서 검색 에러 | status=${res.status}`);
  }

  burstSleep();
}
