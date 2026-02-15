// =====================================================================
// Coupon Load Test: Redisson 분산 락 동시성 — 핵심 시나리오
//
// 목적:
//   - 500개 재고 쿠폰에 600회 동시 요청 → 초과 발급 0건 검증
//   - Redisson 분산 락의 Race Condition 방어 능력 측정
//   - handleSummary로 초과 발급 여부 명시적 PASS/FAIL 출력
//
// 핵심 임계값:
//   - coupon_issue_success_total ≤ 500건 (절대 초과 불가)
//   - 서버 에러(5xx) < 5%
// =====================================================================
import http from 'k6/http';
import { check } from 'k6';
import { Counter, Rate } from 'k6/metrics';
import { BASE_URL, TEST_DATA, makeAuthHeaders, makeHeaders, COUPON_LOAD_THRESHOLDS } from '../../common/config.js';
import {
  getTokenForVU,
  checkStatus,
  userThinkTime,
  burstSleep,
} from '../../common/helpers.js';

// =====================================================================
// 커스텀 메트릭
// =====================================================================

/** 쿠폰 발급 성공 건수 (HTTP 200) — 500건 초과 불가 */
const couponIssueSuccessTotal = new Counter('coupon_issue_success_total');

/** 재고 소진/중복 발급 (HTTP 400/409) — 정상 실패 */
const couponIssueConflictTotal = new Counter('coupon_issue_conflict_total');

/** 서버 에러 (HTTP 5xx) — 비정상 실패 */
const couponIssueFailedTotal = new Counter('coupon_issue_failed_total');

/** 쿠폰 발급 성공률 */
const couponSuccessRate = new Rate('coupon_success_rate');

// =====================================================================
// 테스트 설정
// =====================================================================
export const options = {
  scenarios: {
    // ------------------------------------------------------------------
    // 시나리오 1: 쿠폰 동시 발급 경쟁 (핵심)
    // shared-iterations: 500 VU가 600번 요청을 공유하여 실행
    // 쿠폰 500개 대비 100회 초과 요청 → 초과 발급 0건 검증
    // ------------------------------------------------------------------
    coupon_concurrent_issue: {
      executor: 'shared-iterations',
      vus: 500,
      iterations: 600,
      maxDuration: '3m',
      exec: 'issueCouponScenario',
      gracefulStop: '30s',
      tags: { scenario: 'coupon_concurrent' },
    },

    // ------------------------------------------------------------------
    // 시나리오 2: 쿠폰 정책 조회 (백그라운드 읽기)
    // 쿠폰 발급 중 정책 조회도 안정적으로 처리되는지 확인
    // ------------------------------------------------------------------
    coupon_policy_read: {
      executor: 'constant-vus',
      vus: 20,
      duration: '3m',
      exec: 'readCouponPolicyScenario',
      tags: { scenario: 'coupon_policy_read' },
    },
  },

  thresholds: {
    ...COUPON_LOAD_THRESHOLDS,
    // 쿠폰 발급 요청 전체 응답시간 (락 대기 포함)
    'http_req_duration{scenario:coupon_concurrent}': [
      { threshold: 'p(95)<3000', abortOnFail: false },
    ],
  },
};

// =====================================================================
// 시나리오 1: 쿠폰 발급 (Redisson 분산 락 경쟁)
// =====================================================================
export function issueCouponScenario() {
  const token = getTokenForVU();
  const authHeaders = makeAuthHeaders(token);

  const res = http.post(
    `${BASE_URL}/api/v1/coupons?couponPolicyId=${TEST_DATA.COUPON_POLICY_ID}`,
    null,
    {
      headers: authHeaders,
      tags: { endpoint: 'coupon_issue' },
      // Redisson 락 대기 시간 포함 — 타임아웃을 넉넉히 설정
      timeout: '10s',
    }
  );

  if (res.status === 200) {
    // 발급 성공 — 절대 500건을 초과해서는 안 됨
    couponIssueSuccessTotal.add(1);
    couponSuccessRate.add(true);

  } else if (res.status === 400 || res.status === 409) {
    // 재고 소진(400) 또는 중복 발급 시도(409) — 정상적인 비즈니스 실패
    couponIssueConflictTotal.add(1);
    couponSuccessRate.add(false);

    check(res, {
      'coupon_conflict: 재고 소진 또는 중복 (정상)': (r) =>
        r.status === 400 || r.status === 409,
    });

  } else if (res.status >= 500) {
    // 서버 에러 — 비정상 실패
    couponIssueFailedTotal.add(1);
    couponSuccessRate.add(false);

    console.error(
      `[ERROR] 쿠폰 발급 서버 에러 | VU=${__VU} | status=${res.status} | body=${res.body ? res.body.substring(0, 300) : 'empty'}`
    );
  } else {
    // 기타 상태코드 (401, 403 등)
    couponIssueFailedTotal.add(1);
    couponSuccessRate.add(false);
  }

  burstSleep();
}

// =====================================================================
// 시나리오 2: 쿠폰 정책 조회 (백그라운드)
// 현재 API에 GET /coupons 엔드포인트가 없어 쿠폰 발급 상태만 확인
// =====================================================================
export function readCouponPolicyScenario() {
  const headers = makeHeaders();

  // 현재 구현에서는 별도 정책 조회 API가 없으므로
  // 헬스체크 또는 다른 읽기 작업으로 대체
  // (실제 CouponPolicyApiController가 있다면 해당 엔드포인트 사용)
  const res = http.get(
    `${BASE_URL}/api/v1/videos/${TEST_DATA.VIDEO_IDS[0]}`,
    {
      headers,
      tags: { endpoint: 'bg_read_during_coupon' },
    }
  );

  check(res, {
    'bg_read: 쿠폰 발급 중 다른 API 정상': (r) => r.status === 200,
  });

  userThinkTime();
}

// =====================================================================
// 커스텀 요약 출력: 초과 발급 여부 명시적 PASS/FAIL
// =====================================================================
export function handleSummary(data) {
  const successCount = data.metrics['coupon_issue_success_total']
    ? data.metrics['coupon_issue_success_total'].values.count
    : 0;
  const conflictCount = data.metrics['coupon_issue_conflict_total']
    ? data.metrics['coupon_issue_conflict_total'].values.count
    : 0;
  const failedCount = data.metrics['coupon_issue_failed_total']
    ? data.metrics['coupon_issue_failed_total'].values.count
    : 0;

  const totalRequests = successCount + conflictCount + failedCount;
  const COUPON_STOCK = 500;
  const isOverIssued = successCount > COUPON_STOCK;

  const divider = '='.repeat(60);
  const summary = [
    '',
    divider,
    '  쿠폰 분산 락 동시성 테스트 결과',
    divider,
    `  총 요청 수        : ${totalRequests}건`,
    `  발급 성공         : ${successCount}건`,
    `  재고 소진/중복    : ${conflictCount}건 (정상 실패)`,
    `  서버 에러         : ${failedCount}건 (비정상 실패)`,
    `  쿠폰 재고         : ${COUPON_STOCK}개`,
    '',
    `  초과 발급 여부    : ${isOverIssued ? '⚠ FAIL — ' + (successCount - COUPON_STOCK) + '건 초과 발급!' : 'PASS — 초과 발급 없음'}`,
    divider,
    '',
  ].join('\n');

  // 콘솔 출력 (k6는 stdout을 직접 출력)
  console.log(summary);

  return {
    stdout: summary,
  };
}
