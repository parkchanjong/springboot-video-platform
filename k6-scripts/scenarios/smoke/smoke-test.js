// =====================================================================
// Smoke Test: 기본 동작 확인
// 목적: 배포 후 모든 엔드포인트가 정상 동작하는지 회귀 테스트
// 설정: 2 VU, 2분 지속
// =====================================================================
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, TEST_DATA, makeAuthHeaders, makeHeaders, SMOKE_THRESHOLDS } from '../../common/config.js';
import {
  randomItem,
  getTokenForVU,
  randomVideoTitle,
  randomCommentText,
  checkStatus,
  extractId,
  userThinkTime,
} from '../../common/helpers.js';

export const options = {
  vus: 2,
  duration: '2m',
  thresholds: SMOKE_THRESHOLDS,
};

export default function () {
  const token = getTokenForVU();
  const authHeaders = makeAuthHeaders(token);
  const headers = makeHeaders();

  // ------------------------------------------------------------------
  // 1. 채널 CRUD
  // ------------------------------------------------------------------

  // 채널 생성
  const channelRes = http.post(
    `${BASE_URL}/api/v1/channels`,
    JSON.stringify({
      snippet: {
        title: `스모크 테스트 채널 ${__VU}`,
        description: '스모크 테스트용 채널입니다.',
        thumbnailUrl: 'https://example.com/thumb.jpg',
      },
      contentOwnerId: `owner-smoke-${__VU}`,
    }),
    { headers }
  );
  checkStatus(channelRes, 200, 'channel_create');
  const channelId = extractId(channelRes) || randomItem(TEST_DATA.CHANNEL_IDS);

  sleep(0.3);

  // 채널 조회
  const channelGetRes = http.get(`${BASE_URL}/api/v1/channels/${channelId}`, { headers });
  check(channelGetRes, {
    'channel_get: status 200': (r) => r.status === 200,
    'channel_get: id 존재': (r) => {
      try { return JSON.parse(r.body).id !== undefined; } catch { return false; }
    },
  });

  // 채널 수정
  const channelUpdateRes = http.put(
    `${BASE_URL}/api/v1/channels/${channelId}`,
    JSON.stringify({
      snippet: {
        title: `수정된 채널 ${__VU}`,
        description: '수정된 설명',
        thumbnailUrl: 'https://example.com/thumb-updated.jpg',
      },
      contentOwnerId: `owner-smoke-${__VU}`,
    }),
    { headers }
  );
  checkStatus(channelUpdateRes, 200, 'channel_update');

  userThinkTime();

  // ------------------------------------------------------------------
  // 2. 비디오 CRUD + 조회수 + 좋아요
  // ------------------------------------------------------------------

  // 비디오 생성
  const videoRes = http.post(
    `${BASE_URL}/api/v1/videos`,
    JSON.stringify({
      title: randomVideoTitle(),
      description: '스모크 테스트용 비디오입니다.',
      thumbnailUrl: 'https://example.com/video-thumb.jpg',
      channelId: channelId,
    }),
    { headers }
  );
  checkStatus(videoRes, 200, 'video_create');
  const videoId = extractId(videoRes) || randomItem(TEST_DATA.VIDEO_IDS);

  sleep(0.3);

  // 비디오 단건 조회 (Redis 캐시)
  const videoGetRes = http.get(`${BASE_URL}/api/v1/videos/${videoId}`, { headers });
  check(videoGetRes, {
    'video_get: status 200': (r) => r.status === 200,
    'video_get: id 존재': (r) => {
      try { return JSON.parse(r.body).id !== undefined; } catch { return false; }
    },
  });

  sleep(0.3);

  // 채널별 비디오 목록 조회
  const videoListRes = http.get(
    `${BASE_URL}/api/v1/videos?channelId=${channelId}`,
    { headers }
  );
  check(videoListRes, {
    'video_list: status 200': (r) => r.status === 200,
    'video_list: 배열 반환': (r) => {
      try { return Array.isArray(JSON.parse(r.body)); } catch { return false; }
    },
  });

  sleep(0.3);

  // 조회수 증가 (Redis INCR)
  const viewRes = http.post(`${BASE_URL}/api/v1/videos/${videoId}/view`, null, { headers });
  checkStatus(viewRes, 200, 'video_view');

  sleep(0.3);

  // 비디오 좋아요
  const likeRes = http.post(
    `${BASE_URL}/api/v1/videos/rate?videoId=${videoId}&rating=like`,
    null,
    { headers: authHeaders }
  );
  checkStatus(likeRes, 200, 'video_like');

  sleep(0.3);

  // 좋아요 상태 조회
  const rateGetRes = http.get(
    `${BASE_URL}/api/v1/videos/rate?videoId=${videoId}`,
    { headers: authHeaders }
  );
  check(rateGetRes, {
    'video_rate_get: status 200': (r) => r.status === 200,
    'video_rate_get: videoId 존재': (r) => {
      try { return JSON.parse(r.body).videoId !== undefined; } catch { return false; }
    },
  });

  userThinkTime();

  // ------------------------------------------------------------------
  // 3. 댓글 CRUD
  // ------------------------------------------------------------------
  const seedVideoId = randomItem(TEST_DATA.VIDEO_IDS);
  const seedChannelId = randomItem(TEST_DATA.CHANNEL_IDS);

  // 댓글 작성
  const commentRes = http.post(
    `${BASE_URL}/api/v1/comments`,
    JSON.stringify({
      channelId: seedChannelId,
      videoId: seedVideoId,
      parentId: null,
      text: randomCommentText(),
    }),
    { headers: authHeaders }
  );
  checkStatus(commentRes, 200, 'comment_create');
  const commentId = extractId(commentRes) || randomItem(TEST_DATA.COMMENT_IDS);

  sleep(0.3);

  // 댓글 단건 조회
  const commentGetRes = http.get(
    `${BASE_URL}/api/v1/comments?commentId=${commentId}`,
    { headers: authHeaders }
  );
  check(commentGetRes, {
    'comment_get: status 200': (r) => r.status === 200,
  });

  sleep(0.3);

  // 댓글 목록 조회 (MongoDB 인덱스 스캔)
  const offset = new Date().toISOString();
  const commentListRes = http.get(
    `${BASE_URL}/api/v1/comments/list?videoId=${seedVideoId}&order=time&offset=${encodeURIComponent(offset)}&maxSize=10`,
    { headers: authHeaders }
  );
  check(commentListRes, {
    'comment_list: status 200': (r) => r.status === 200,
    'comment_list: 배열 반환': (r) => {
      try { return Array.isArray(JSON.parse(r.body)); } catch { return false; }
    },
  });

  sleep(0.3);

  // 대댓글 목록 조회
  const parentId = randomItem(TEST_DATA.PARENT_COMMENT_IDS);
  const replyListRes = http.get(
    `${BASE_URL}/api/v1/comments/reply?parentId=${parentId}&offset=${encodeURIComponent(offset)}&maxSize=10`,
    { headers }
  );
  check(replyListRes, {
    'comment_reply_list: status 200': (r) => r.status === 200,
  });

  sleep(0.3);

  // 댓글 수정
  const commentUpdateRes = http.put(
    `${BASE_URL}/api/v1/comments/${commentId}`,
    JSON.stringify({
      channelId: seedChannelId,
      videoId: seedVideoId,
      parentId: null,
      text: '[수정됨] ' + randomCommentText(),
    }),
    { headers: authHeaders }
  );
  checkStatus(commentUpdateRes, 200, 'comment_update');

  sleep(0.3);

  // 댓글 삭제
  const commentDeleteRes = http.del(
    `${BASE_URL}/api/v1/comments/${commentId}`,
    null,
    { headers: authHeaders }
  );
  checkStatus(commentDeleteRes, 200, 'comment_delete');

  userThinkTime();

  // ------------------------------------------------------------------
  // 4. 구독
  // ------------------------------------------------------------------

  // 채널 구독
  const subscribeRes = http.post(
    `${BASE_URL}/api/v1/subscribe?channelId=${seedChannelId}`,
    null,
    { headers: authHeaders }
  );
  checkStatus(subscribeRes, 200, 'subscribe');
  const subscribeId = extractId(subscribeRes) || randomItem(TEST_DATA.SUBSCRIBE_IDS);

  sleep(0.3);

  // 구독 채널 목록 조회
  const subscribeListRes = http.get(
    `${BASE_URL}/api/v1/subscribe/mine`,
    { headers: authHeaders }
  );
  check(subscribeListRes, {
    'subscribe_list: status 200': (r) => r.status === 200,
  });

  sleep(0.3);

  // 구독 취소
  const unsubscribeRes = http.del(
    `${BASE_URL}/api/v1/subscribe?subscribeId=${subscribeId}`,
    null,
    { headers: authHeaders }
  );
  checkStatus(unsubscribeRes, 200, 'unsubscribe');

  userThinkTime();

  // ------------------------------------------------------------------
  // 5. 쿠폰 발급
  // ------------------------------------------------------------------
  const couponRes = http.post(
    `${BASE_URL}/api/v1/coupons?couponPolicyId=${TEST_DATA.COUPON_POLICY_ID}`,
    null,
    { headers: authHeaders }
  );
  // 스모크 테스트에서는 200(성공) 또는 400/409(재고 소진/중복)를 모두 허용
  check(couponRes, {
    'coupon_issue: 서버 에러 없음': (r) => r.status < 500,
  });

  userThinkTime();

  // ------------------------------------------------------------------
  // 6. 도서 검색 (외부 API + Resilience4j Circuit Breaker)
  // ------------------------------------------------------------------
  const bookRes = http.get(
    `${BASE_URL}/api/v1/book/list?query=spring+boot&page=1&size=5`,
    { headers }
  );
  // Circuit Breaker 동작 시 503 응답 가능 → 모두 허용
  check(bookRes, {
    'book_search: 서버 에러 없음': (r) => r.status < 500 || r.status === 503,
  });

  userThinkTime();
}
