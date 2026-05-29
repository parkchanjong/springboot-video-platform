// 비디오 상세 Redis 캐시 stampede protection 효과를 측정합니다.
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { BASE_URL, TEST_DATA, makeHeaders } from '../../common/config.js';

const concurrency = Number(__ENV.STAMPEDE_CONCURRENCY || 200);
const cacheTtlSeconds = Number(__ENV.CACHE_TTL_SECONDS || 3);
const hotVideoId = __ENV.HOT_VIDEO_ID || TEST_DATA.VIDEO_IDS[0];

const stampedeVideoGetDuration = new Trend('stampede_video_get_duration', true);
const stampedeSuccessTotal = new Counter('stampede_success_total');
const stampedeFailedTotal = new Counter('stampede_failed_total');

export const options = {
  scenarios: {
    cache_stampede_burst: {
      executor: 'per-vu-iterations',
      vus: concurrency,
      iterations: 1,
      maxDuration: '30s',
      exec: 'cacheStampedeBurst',
      tags: { scenario: 'cache_stampede_burst' },
    },
  },
  thresholds: {
    http_req_failed: [{ threshold: 'rate<0.01', abortOnFail: false }],
    stampede_success_total: [{ threshold: `count>=${Math.floor(concurrency * 0.99)}`, abortOnFail: false }],
  },
};

const headers = makeHeaders();

export function setup() {
  const warmUpRes = http.get(`${BASE_URL}/api/v1/videos/${hotVideoId}`, {
    headers,
    tags: { endpoint: 'video_cache_warmup' },
    timeout: '10s',
  });

  check(warmUpRes, {
    'video_cache_warmup: status 200': (r) => r.status === 200,
  });

  sleep(cacheTtlSeconds + 0.2);
}

export function cacheStampedeBurst() {
  const res = http.get(`${BASE_URL}/api/v1/videos/${hotVideoId}`, {
    headers,
    tags: { endpoint: 'video_cache_stampede' },
    timeout: '10s',
  });

  stampedeVideoGetDuration.add(res.timings.duration);

  if (check(res, { 'video_cache_stampede: status 200': (r) => r.status === 200 })) {
    stampedeSuccessTotal.add(1);
    return;
  }

  stampedeFailedTotal.add(1);
}
