# AGENTS.md

Governance for `k6-scripts`.

## Scope

- Applies to k6 common helpers, shared config, and scenario scripts.
- Scripts validate runtime behavior and performance contracts. Keep them aligned with API routes and seeded data.

## Configuration Rules

- Use `BASE_URL` from `k6-scripts/common/config.js`. Override it with the `BASE_URL` environment variable.
- Use `TEST_DATA` for seeded IDs and auth tokens. Do not scatter seed constants across scenarios.
- Authenticated requests must use `x-auth-key` through the shared header helpers.
- Keep scenario thresholds in shared config unless a scenario needs local custom metrics.

## Scenario Structure

- Keep smoke tests broad and low volume.
- Keep load tests focused on a single subsystem such as Redis video cache, MongoDB comments, or Redisson coupon locking.
- Keep stress and spike tests explicit about traffic mix and expected threshold behavior.
- Reuse helpers for random data, offset generation, request headers, and checks.

## Coupon Over-Issue Validation

- Coupon load tests must keep `coupon_issue_success_total` at or below the policy quantity.
- Treat stock exhaustion or duplicate issue responses as expected conflict behavior when the API uses that contract.
- Server errors are separate from expected conflicts and must remain under the scenario threshold.

## Execution

- Smoke. `k6 run k6-scripts/scenarios/smoke/smoke-test.js`
- Video load. `BASE_URL=http://localhost:8080 k6 run k6-scripts/scenarios/load/video-load.js`
- Comment load. `BASE_URL=http://localhost:8080 k6 run k6-scripts/scenarios/load/comment-load.js`
- Coupon load. `BASE_URL=http://localhost:8080 k6 run k6-scripts/scenarios/load/coupon-load.js`
- Stress. `BASE_URL=http://localhost:8080 k6 run k6-scripts/scenarios/stress/stress-test.js`
- Spike. `BASE_URL=http://localhost:8080 k6 run k6-scripts/scenarios/spike/spike-test.js`
