// =====================================================================
// Stress Test: 시스템 한계점 탐색
//
// 목적:
//   - 단계적 VU 증가로 응답시간 저하 시작 지점 탐색
//   - 각 VU 단계에서 P95/P99 추이 측정
//   - 부하 감소 후 회복력 확인
//
// 워크로드 분배 (실제 트래픽 패턴):
//   비디오 조회 40% | 댓글 목록 30% | 조회수 증가 15% | 댓글 작성 10% | 구독·좋아요 5%
//
// 단계: 0→50→100→200→300→400→500→300→100→0 (총 약 23분)
// =====================================================================
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import { BASE_URL, TEST_DATA, makeAuthHeaders, makeHeaders, STRESS_THRESHOLDS } from '../../common/config.js';
import {
  randomItem,
  getTokenForVU,
  randomCommentText,
  generateCursorOffset,
  checkStatus,
  userThinkTime,
  burstSleep,
  paretoVideoId,
} from '../../common/helpers.js';

// =====================================================================
// 커스텀 메트릭
// =====================================================================
const stressVideoGetDuration = new Trend('stress_video_get_duration', true);
const stressCommentListDuration = new Trend('stress_comment_list_duration', true);
const stressErrorTotal = new Counter('stress_error_total');

// =====================================================================
// 테스트 설정
// =====================================================================
export const options = {
  stages: [
    { duration: '2m', target: 50 },   // 1단계: 워밍업
    { duration: '2m', target: 100 },  // 2단계: 기본 부하
    { duration: '2m', target: 200 },  // 3단계: 중간 부하
    { duration: '3m', target: 300 },  // 4단계: 높은 부하
    { duration: '3m', target: 400 },  // 5단계: 매우 높은 부하
    { duration: '3m', target: 500 },  // 6단계: 최대 부하 (한계점 탐색)
    { duration: '2m', target: 300 },  // 7단계: 부하 감소
    { duration: '2m', target: 100 },  // 8단계: 회복 확인
    { duration: '2m', target: 0 },    // 9단계: 쿨다운
  ],

  thresholds: {
    ...STRESS_THRESHOLDS,
    // 스트레스 시나리오별 개별 임계값
    stress_video_get_duration: [{ threshold: 'p(99)<2000', abortOnFail: false }],
    stress_comment_list_duration: [{ threshold: 'p(99)<2000', abortOnFail: false }],
  },
};

// =====================================================================
// 메인 시나리오: 확률적 워크로드 분배
// =====================================================================
export default function () {
  const rand = Math.random();

  if (rand < 0.40) {
    // 40%: 비디오 조회 (Redis 캐시)
    doVideoGet();
  } else if (rand < 0.70) {
    // 30%: 댓글 목록 조회 (MongoDB)
    doCommentList();
  } else if (rand < 0.85) {
    // 15%: 조회수 증가 (Redis INCR)
    doViewCount();
  } else if (rand < 0.95) {
    // 10%: 댓글 작성 (MongoDB 쓰기)
    doCommentCreate();
  } else {
    // 5%: 구독 또는 좋아요 (MySQL + Redis)
    if (Math.random() < 0.5) {
      doLikeVideo();
    } else {
      doSubscribe();
    }
  }
}

// =====================================================================
// 워크로드 함수
// =====================================================================

/** 비디오 단건 조회 (파레토 분포: 인기 비디오 집중) */
function doVideoGet() {
  const headers = makeHeaders();
  const videoId = paretoVideoId();

  const res = http.get(
    `${BASE_URL}/api/v1/videos/${videoId}`,
    {
      headers,
      tags: { endpoint: 'stress_video_get' },
    }
  );

  if (res.status !== 200) {
    stressErrorTotal.add(1);
    console.error(`[STRESS] video_get 실패 | VU=${__VU} | status=${res.status}`);
  }

  stressVideoGetDuration.add(res.timings.duration);
  burstSleep();
}

/** 댓글 목록 조회 (커서 페이지네이션) */
function doCommentList() {
  const token = getTokenForVU();
  const authHeaders = makeAuthHeaders(token);

  const videoId = randomItem(TEST_DATA.VIDEO_IDS);
  const offset = generateCursorOffset();

  const res = http.get(
    `${BASE_URL}/api/v1/comments/list?videoId=${videoId}&order=time&offset=${encodeURIComponent(offset)}&maxSize=20`,
    {
      headers: authHeaders,
      tags: { endpoint: 'stress_comment_list' },
    }
  );

  if (res.status !== 200) {
    stressErrorTotal.add(1);
  }

  stressCommentListDuration.add(res.timings.duration);
  burstSleep();
}

/** 조회수 증가 (Redis 원자성) */
function doViewCount() {
  const headers = makeHeaders();
  const videoId = randomItem(TEST_DATA.VIDEO_IDS);

  const res = http.post(
    `${BASE_URL}/api/v1/videos/${videoId}/view`,
    null,
    {
      headers,
      tags: { endpoint: 'stress_view_count' },
    }
  );

  if (res.status !== 200) {
    stressErrorTotal.add(1);
  }

  burstSleep();
}

/** 댓글 작성 (MongoDB 쓰기) */
function doCommentCreate() {
  const token = getTokenForVU();
  const authHeaders = makeAuthHeaders(token);

  const videoId = randomItem(TEST_DATA.VIDEO_IDS);
  const channelId = randomItem(TEST_DATA.CHANNEL_IDS);

  const res = http.post(
    `${BASE_URL}/api/v1/comments`,
    JSON.stringify({
      channelId: channelId,
      videoId: videoId,
      parentId: null,
      text: randomCommentText(),
    }),
    {
      headers: authHeaders,
      tags: { endpoint: 'stress_comment_create' },
      timeout: '15s',
    }
  );

  if (res.status !== 200) {
    stressErrorTotal.add(1);
  }

  burstSleep();
}

/** 비디오 좋아요 (MySQL + Redis) */
function doLikeVideo() {
  const token = getTokenForVU();
  const authHeaders = makeAuthHeaders(token);

  const videoId = randomItem(TEST_DATA.VIDEO_IDS);

  const res = http.post(
    `${BASE_URL}/api/v1/videos/rate?videoId=${videoId}&rating=like`,
    null,
    {
      headers: authHeaders,
      tags: { endpoint: 'stress_like_video' },
    }
  );

  if (res.status !== 200) {
    stressErrorTotal.add(1);
  }

  burstSleep();
}

/** 채널 구독 */
function doSubscribe() {
  const token = getTokenForVU();
  const authHeaders = makeAuthHeaders(token);

  const channelId = randomItem(TEST_DATA.CHANNEL_IDS);

  const res = http.post(
    `${BASE_URL}/api/v1/subscribe?channelId=${channelId}`,
    null,
    {
      headers: authHeaders,
      tags: { endpoint: 'stress_subscribe' },
    }
  );

  // 이미 구독 중인 경우 409도 정상으로 처리
  if (res.status >= 500) {
    stressErrorTotal.add(1);
  }

  burstSleep();
}
