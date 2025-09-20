# Repository Guidelines

## Project Structure & Module Organization
`video-info-manager` is a multi-module Gradle project targeting Java 17. Runtime apps live under `video-apps`: `app-api` exposes the Spring Boot HTTP layer, while `app-batch` ships scheduled and offline flows. Core domain logic is split across `video-core` (`core-domain`, `core-port`, `core-service`, `core-usecase`), with adapters and shared utilities in `video-adapters` and `video-commons`. Observability lives in `support/monitoring`. Conventional sources reside in `src/main/java` and tests in `src/test/java`; reuse cross-cutting fixtures from `testFixtures`. Infrastructure and local stacks sit under `infra/` (Docker Compose, databases, monitoring).

## Build, Test, and Development Commands
Run Gradle via the wrapper to ensure plugin parity:
```bash
./gradlew clean build          # compile all modules and run unit tests
./gradlew test                 # quick feedback on unit tests only
./gradlew integrationTest      # execute integration suite when present
./gradlew :video-apps:app-api:bootRun   # start the REST API locally
./gradlew :video-apps:app-batch:bootJar # package batch app for distribution
```

## Coding Style & Naming Conventions
Follow idiomatic Java conventions with 4-space indentation and PascalCase types. Domain interfaces go in `core-port` (`*Port` suffix) and implementations in `core-service` (`*Service`). Prefer constructor injection, Lombok annotations, and MapStruct mappers where appropriate. Keep package names under `com.videoservice` aligned with module boundaries, and document behaviors with `@DisplayName` on tests.

## Testing Guidelines
JUnit 5, AssertJ, Mockito, and Fixture Monkey are the standard stack. Mirror production package paths in `src/test/java` and use `Given_When_Then` method names for clarity. Place heavier integration scenarios in `src/integrationTest/java` so they run via `integrationTest`. Maintain deterministic data by stubbing outbound ports and leveraging test fixtures from `testFixtures`.

## Commit & Pull Request Guidelines
Match the repository commit taxonomy: `feat`, `fix`, `refactor`, `style`, `chore`, `docs`, `rename`, `remove`. Start messages with the tag, keep the subject under 50 characters, and add concise context in the body when needed (English or Korean acceptable). Before opening a PR, ensure tests pass, describe the change set, reference issues, and attach REST Docs snippets or screenshots when UI/API changes apply. Highlight follow-up work and note any configuration toggles touched.

## Security & Configuration Tips
Never commit credentials; rely on `.env` or secrets managers referenced by Docker in `infra/`. When adding monitoring or persistence integrations, reuse the existing Prometheus/Grafana and database containers to keep environments reproducible.
