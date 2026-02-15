// =====================================================================
// Comment Load Test: MongoDB CRUD 성능
// 목적:
//   - 댓글 읽기(70%) / 쓰기(20%) / 수정·삭제(10%) 비율 시뮬레이션
//   - MongoDB 인덱스 스캔 + 커서 페이지네이션 성능 측정
//   - 대댓글(30% 확률) 생성 포함
// =====================================================================
import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';
import { BASE_URL, TEST_DATA, makeAuthHeaders, makeHeaders, COMMENT_LOAD_THRESHOLDS } from '../../common/config.js';
import {
  randomItem,
  getTokenForVU,
  randomCommentText,
  generateCursorOffset,
  checkStatus,
  extractId,
  userThinkTime,
  burstSleep,
} from '../../common/helpers.js';

// =====================================================================
// 커스텀 메트릭
// =====================================================================

/** 댓글 생성 응답시간 */
const commentCreateDuration = new Trend('comment_create_duration', true);

/** 댓글 목록 조회 응답시간 */
const commentListDuration = new Trend('comment_list_duration', true);

/** 대댓글 목록 조회 응답시간 */
const commentReplyDuration = new Trend('comment_reply_duration', true);

// =====================================================================
// 테스트 설정: 3개 시나리오 (읽기 70% / 쓰기 20% / 수정·삭제 10%)
// =====================================================================
export const options = {
  scenarios: {
    // ------------------------------------------------------------------
    // 시나리오 1: 댓글 읽기 (전체 트래픽의 70%)
    // ------------------------------------------------------------------
    comment_read: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 20 },  // 워밍업
        { duration: '2m', target: 70 },  // 목표 VU 도달
        { duration: '3m', target: 70 },  // 유지
        { duration: '1m', target: 0 },   // 쿨다운
      ],
      exec: 'readCommentScenario',
      gracefulRampDown: '30s',
      tags: { scenario: 'comment_read' },
    },

    // ------------------------------------------------------------------
    // 시나리오 2: 댓글 쓰기 (전체 트래픽의 20%)
    // 30% 확률로 대댓글 작성
    // ------------------------------------------------------------------
    comment_write: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 5 },   // 워밍업
        { duration: '2m', target: 20 },  // 목표 VU 도달
        { duration: '3m', target: 20 },  // 유지
        { duration: '1m', target: 0 },   // 쿨다운
      ],
      exec: 'writeCommentScenario',
      gracefulRampDown: '30s',
      tags: { scenario: 'comment_write' },
    },

    // ------------------------------------------------------------------
    // 시나리오 3: 댓글 수정·삭제 (전체 트래픽의 10%)
    // 작성 → 수정 → 삭제 전체 흐름
    // ------------------------------------------------------------------
    comment_modify: {
      executor: 'constant-vus',
      vus: 5,
      duration: '7m',
      exec: 'modifyCommentScenario',
      tags: { scenario: 'comment_modify' },
    },
  },

  thresholds: {
    ...COMMENT_LOAD_THRESHOLDS,
    comment_create_duration: [{ threshold: 'p(95)<200', abortOnFail: false }],
    comment_list_duration: [{ threshold: 'p(95)<150', abortOnFail: false }],
    comment_reply_duration: [{ threshold: 'p(95)<150', abortOnFail: false }],
  },
};

// =====================================================================
// 시나리오 1: 댓글 읽기
// =====================================================================
export function readCommentScenario() {
  const token = getTokenForVU();
  const authHeaders = makeAuthHeaders(token);
  const headers = makeHeaders();

  const videoId = randomItem(TEST_DATA.VIDEO_IDS);
  const offset = generateCursorOffset();

  // 댓글 목록 조회 (커서 기반 페이지네이션)
  const listRes = http.get(
    `${BASE_URL}/api/v1/comments/list?videoId=${videoId}&order=time&offset=${encodeURIComponent(offset)}&maxSize=20`,
    {
      headers: authHeaders,
      tags: { endpoint: 'comment_list' },
    }
  );

  check(listRes, {
    'comment_list: status 200': (r) => r.status === 200,
    'comment_list: 배열 반환': (r) => {
      try { return Array.isArray(JSON.parse(r.body)); } catch { return false; }
    },
  });
  commentListDuration.add(listRes.timings.duration);

  userThinkTime();

  // 50% 확률로 대댓글도 조회
  if (Math.random() < 0.5) {
    const parentId = randomItem(TEST_DATA.PARENT_COMMENT_IDS);
    const replyOffset = generateCursorOffset();

    const replyRes = http.get(
      `${BASE_URL}/api/v1/comments/reply?parentId=${parentId}&offset=${encodeURIComponent(replyOffset)}&maxSize=10`,
      {
        headers,
        tags: { endpoint: 'comment_reply' },
      }
    );

    check(replyRes, {
      'comment_reply: status 200': (r) => r.status === 200,
    });
    commentReplyDuration.add(replyRes.timings.duration);

    userThinkTime();
  }

  // 20% 확률로 단건 댓글 조회
  if (Math.random() < 0.2) {
    const commentId = randomItem(TEST_DATA.COMMENT_IDS);
    const commentGetRes = http.get(
      `${BASE_URL}/api/v1/comments?commentId=${commentId}`,
      {
        headers: authHeaders,
        tags: { endpoint: 'comment_get' },
      }
    );
    check(commentGetRes, {
      'comment_get: status 200': (r) => r.status === 200,
    });
  }
}

// =====================================================================
// 시나리오 2: 댓글 쓰기 (30% 확률 대댓글)
// =====================================================================
export function writeCommentScenario() {
  const token = getTokenForVU();
  const authHeaders = makeAuthHeaders(token);

  const videoId = randomItem(TEST_DATA.VIDEO_IDS);
  const channelId = randomItem(TEST_DATA.CHANNEL_IDS);

  // 30% 확률로 대댓글 작성
  const isReply = Math.random() < 0.3;
  const parentId = isReply ? randomItem(TEST_DATA.PARENT_COMMENT_IDS) : null;

  const res = http.post(
    `${BASE_URL}/api/v1/comments`,
    JSON.stringify({
      channelId: channelId,
      videoId: videoId,
      parentId: parentId,
      text: randomCommentText(),
    }),
    {
      headers: authHeaders,
      tags: { endpoint: 'comment_create' },
      timeout: '10s',
    }
  );

  checkStatus(res, 200, isReply ? 'reply_create' : 'comment_create');
  commentCreateDuration.add(res.timings.duration);

  userThinkTime();
}

// =====================================================================
// 시나리오 3: 댓글 수정·삭제 (작성 → 수정 → 삭제 전체 흐름)
// =====================================================================
export function modifyCommentScenario() {
  const token = getTokenForVU();
  const authHeaders = makeAuthHeaders(token);

  const videoId = randomItem(TEST_DATA.VIDEO_IDS);
  const channelId = randomItem(TEST_DATA.CHANNEL_IDS);

  // Step 1: 댓글 작성
  const createRes = http.post(
    `${BASE_URL}/api/v1/comments`,
    JSON.stringify({
      channelId: channelId,
      videoId: videoId,
      parentId: null,
      text: randomCommentText(),
    }),
    {
      headers: authHeaders,
      tags: { endpoint: 'comment_create_for_modify' },
    }
  );

  if (!checkStatus(createRes, 200, 'comment_create_for_modify')) {
    userThinkTime();
    return;
  }

  const commentId = extractId(createRes);
  if (!commentId) {
    userThinkTime();
    return;
  }

  burstSleep();

  // Step 2: 댓글 수정
  const updateRes = http.put(
    `${BASE_URL}/api/v1/comments/${commentId}`,
    JSON.stringify({
      channelId: channelId,
      videoId: videoId,
      parentId: null,
      text: '[수정됨] ' + randomCommentText(),
    }),
    {
      headers: authHeaders,
      tags: { endpoint: 'comment_update' },
    }
  );
  checkStatus(updateRes, 200, 'comment_update');

  userThinkTime();

  // Step 3: 댓글 삭제
  const deleteRes = http.del(
    `${BASE_URL}/api/v1/comments/${commentId}`,
    null,
    {
      headers: authHeaders,
      tags: { endpoint: 'comment_delete' },
    }
  );
  checkStatus(deleteRes, 200, 'comment_delete');

  userThinkTime();
}
