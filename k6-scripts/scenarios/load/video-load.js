// =====================================================================
// Video Load Test: Redis 캐시 성능 + 조회수 동시성
// 목적:
//   - 파레토 분포로 인기 비디오 집중 조회 → Redis 캐시 히트율 측정
//   - 동일 비디오 동시 INCR → Redis 원자성 검증
//   - 비디오 생성 → Kafka 이벤트 발행 포함
// =====================================================================
import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { BASE_URL, TEST_DATA, makeHeaders, VIDEO_LOAD_THRESHOLDS } from '../../common/config.js';
import {
  randomItem,
  randomVideoTitle,
  checkStatus,
  extractId,
  userThinkTime,
  burstSleep,
  paretoVideoId,
} from '../../common/helpers.js';

// =====================================================================
// 커스텀 메트릭
// =====================================================================

/** 캐시 히트율: 50ms 미만 응답 = Redis 캐시 히트로 판정 */
const videoCacheHitRate = new Rate('video_cache_hit_rate');

/** 비디오 단건 조회 응답시간 */
const videoGetDuration = new Trend('video_get_duration', true);

/** 조회수 증가 응답시간 */
const videoViewDuration = new Trend('video_view_duration', true);

/** 비디오 생성 성공 건수 */
const videoCreateTotal = new Counter('video_create_total');

// =====================================================================
// 테스트 설정: 3개 시나리오 병렬 실행
// =====================================================================
export const options = {
  scenarios: {
    // ------------------------------------------------------------------
    // 시나리오 1: 비디오 읽기 헤비 (파레토 분포)
    // 실제 서비스: 상위 20% 비디오가 80% 트래픽을 받는 패턴
    // ------------------------------------------------------------------
    video_read_heavy: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 30 },   // 워밍업
        { duration: '2m', target: 100 },  // 부하 증가
        { duration: '2m', target: 150 },  // 최대 부하 유지
        { duration: '1m', target: 50 },   // 감소
        { duration: '1m', target: 0 },    // 쿨다운
      ],
      exec: 'readVideoScenario',
      gracefulRampDown: '30s',
      tags: { scenario: 'video_read_heavy' },
    },

    // ------------------------------------------------------------------
    // 시나리오 2: 조회수 버스트 (Redis 핫키 동시성)
    // 동일 비디오에 집중 → INCR 원자성 및 성능 측정
    // ------------------------------------------------------------------
    view_count_burst: {
      executor: 'constant-vus',
      vus: 50,
      duration: '5m',
      startTime: '1m', // video_read_heavy 워밍업 후 시작
      exec: 'viewCountBurstScenario',
      tags: { scenario: 'view_count_burst' },
    },

    // ------------------------------------------------------------------
    // 시나리오 3: 비디오 생성 (Kafka 비동기 이벤트 발행)
    // constant-arrival-rate: 초당 5회 생성 요청
    // ------------------------------------------------------------------
    video_create: {
      executor: 'constant-arrival-rate',
      rate: 5,
      timeUnit: '1s',
      duration: '5m',
      preAllocatedVUs: 20,
      maxVUs: 50,
      startTime: '2m', // 시스템 안정 후 시작
      exec: 'createVideoScenario',
      tags: { scenario: 'video_create' },
    },
  },

  thresholds: {
    ...VIDEO_LOAD_THRESHOLDS,
    // 조회수 증가는 캐시와 무관하게 빠르게 처리되어야 함
    video_view_duration: [{ threshold: 'p(95)<50', abortOnFail: false }],
  },
};

const headers = makeHeaders();

// =====================================================================
// 시나리오 1: 비디오 읽기 (파레토 분포 적용)
// =====================================================================
export function readVideoScenario() {
  // 파레토 분포: 상위 3개 비디오에 80% 집중
  const videoId = paretoVideoId();

  const res = http.get(
    `${BASE_URL}/api/v1/videos/${videoId}`,
    {
      headers,
      tags: { endpoint: 'video_get' },
    }
  );

  const passed = checkStatus(res, 200, 'video_get');

  // 커스텀 메트릭 기록
  videoGetDuration.add(res.timings.duration);

  // 50ms 미만 = Redis 캐시 히트 판정
  videoCacheHitRate.add(res.timings.duration < 50);

  // 10% 확률로 채널별 비디오 목록도 조회 (다양한 액세스 패턴)
  if (Math.random() < 0.1) {
    const channelId = randomItem(TEST_DATA.CHANNEL_IDS);
    const listRes = http.get(
      `${BASE_URL}/api/v1/videos?channelId=${channelId}`,
      {
        headers,
        tags: { endpoint: 'video_list' },
      }
    );
    check(listRes, {
      'video_list: status 200': (r) => r.status === 200,
    });
  }

  userThinkTime();
}

// =====================================================================
// 시나리오 2: 조회수 버스트 (동일 비디오 집중 INCR)
// =====================================================================
export function viewCountBurstScenario() {
  // 핫 비디오 1개에 모든 요청 집중 → Redis 핫키 성능 측정
  const hotVideoId = TEST_DATA.VIDEO_IDS[0];

  const res = http.post(
    `${BASE_URL}/api/v1/videos/${hotVideoId}/view`,
    null,
    {
      headers,
      tags: { endpoint: 'video_view' },
      timeout: '5s',
    }
  );

  checkStatus(res, 200, 'video_view');
  videoViewDuration.add(res.timings.duration);

  burstSleep(); // 연속 요청 간 최소 대기
}

// =====================================================================
// 시나리오 3: 비디오 생성 (Kafka 이벤트 발행 포함)
// =====================================================================
export function createVideoScenario() {
  const channelId = randomItem(TEST_DATA.CHANNEL_IDS);

  const res = http.post(
    `${BASE_URL}/api/v1/videos`,
    JSON.stringify({
      title: randomVideoTitle(),
      description: 'k6 부하 테스트 생성 비디오',
      thumbnailUrl: 'https://example.com/load-test-thumb.jpg',
      channelId: channelId,
    }),
    {
      headers,
      tags: { endpoint: 'video_create' },
      timeout: '10s', // Kafka 발행 대기 포함
    }
  );

  if (checkStatus(res, 200, 'video_create')) {
    videoCreateTotal.add(1);
  }

  burstSleep();
}
