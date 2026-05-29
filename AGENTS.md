# AGENTS.md

Repository governance for coding agents working in `springboot-video-platform`.

## Scope

- Applies to the whole repository unless a nested `AGENTS.md` overrides or narrows a rule.
- Keep this file operational. Do not duplicate README.MD feature narratives, architecture diagrams, or k6 result tables here.
- Prefer the closest nested governance file when editing high-context modules.

## Operating Commands

- Full build. `./gradlew build`
- All tests. `./gradlew test`
- API tests. `./gradlew :video-apps:app-api:test`
- Batch tests. `./gradlew :video-apps:app-batch:test`
- Local infrastructure. `docker compose -f infra/docker-compose.yml up -d`
- k6 smoke. `k6 run k6-scripts/scenarios/smoke/smoke-test.js`
- k6 video load. `k6 run k6-scripts/scenarios/load/video-load.js`
- k6 comment load. `k6 run k6-scripts/scenarios/load/comment-load.js`
- k6 coupon load. `k6 run k6-scripts/scenarios/load/coupon-load.js`
- k6 stress. `k6 run k6-scripts/scenarios/stress/stress-test.js`
- k6 spike. `k6 run k6-scripts/scenarios/spike/spike-test.js`

Use the smallest relevant command first, then broaden verification when the change crosses module boundaries.

## Golden Rules

- Java version is 17. Do not introduce APIs or toolchains requiring a newer runtime.
- Build files use Gradle Kotlin DSL. Keep module wiring in `settings.gradle.kts` and `*.gradle.kts`.
- Spring Boot version is 3.3.3 via `buildSrc/src/main/kotlin/Versions.kt`.
- Preserve the hexagonal dependency direction. Core defines business intent, adapters implement infrastructure, apps expose entry points.
- Do not hard-code secrets, tokens, credentials, host-specific paths, or private endpoints.
- Do not duplicate README.MD content. Link or summarize only when the governance context needs it.
- Keep QueryDSL inside persistence concerns and generated query usage. Do not leak QueryDSL types into core use cases or controllers.
- Respect Redis, Kafka, MongoDB, and MySQL boundaries. Pick the storage or messaging technology that already owns the behavior.
- Prefer existing ports, use cases, services, DTOs, and test fixtures before adding new abstractions.
- Make surgical changes. Avoid broad reformatting or unrelated cleanup.

## Architecture Map

- `video-core:core-domain` owns domain models and domain-level behavior.
- `video-core:core-usecase` owns use case contracts and command objects.
- `video-core:core-port` owns outbound interfaces required by core services.
- `video-core:core-service` implements use cases using domain models and ports.
- `video-adapters` implements persistence, cache, messaging, distributed lock, and external API ports.
- `video-apps/app-api` exposes REST controllers, request/response DTOs, authentication argument resolution, and API documentation tests.
- `video-apps/app-batch` runs Spring Batch jobs, currently including Redis view-count synchronization.
- `video-commons` contains shared utilities only. Keep business rules out of commons.
- `support` modules provide logging and monitoring support.
- `testFixtures` and `tests/api-docs` provide shared test infrastructure.
- `infra` contains local infrastructure configuration.
- `k6-scripts` contains load, smoke, stress, and spike scenarios.

## Commit Rules

- Commit one logical change at a time when the work is complete and verified.
- Use the README.MD convention tags where relevant. `feat`, `fix`, `refactor`, `style`, `chore`, `docs`, `rename`, `remove`.
- Keep unrelated dirty worktree changes out of your commit.
- Do not rewrite history or discard user changes unless explicitly asked.

## Context Map

- For core domain, use case, service, or port edits, read `video-core/AGENTS.md`.
- For persistence, Redis, Kafka, MongoDB, Feign, QueryDSL, or Redisson edits, read `video-adapters/AGENTS.md`.
- For REST API, controller, DTO, auth header, or API docs tests, read `video-apps/app-api/AGENTS.md`.
- For Spring Batch jobs or Redis view-count sync, read `video-apps/app-batch/AGENTS.md`.
- For performance scenarios and k6 data contracts, read `k6-scripts/AGENTS.md`.
