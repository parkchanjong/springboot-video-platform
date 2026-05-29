# AGENTS.md

Governance for `video-apps/app-batch`.

## Scope

- Applies to Spring Batch application wiring, job configuration, listeners, and batch tests.
- This module is a batch entry point. Keep business logic in core services or ports.

## Batch Job Rules

- Define jobs and steps with explicit names that describe the batch operation.
- Keep `JobRepository`, transaction manager, listener, and tasklet wiring inside batch configuration.
- Do not make batch configuration depend on web controllers or API DTOs.
- Job parameters should be explicit and repeatable for tests and operations.

## View Count Sync

- Redis owns high-frequency view count increments during request traffic.
- Batch sync is responsible for draining or reading accumulated Redis view counts and applying them to MySQL through the appropriate port.
- Keep Redis key knowledge in adapters. Batch code should call ports rather than constructing infrastructure-specific keys.
- Preserve idempotency expectations when retrying a sync step.

## Tests

- Use Spring Batch test support for job and step behavior.
- Verify job names, step execution, and port interactions for batch logic.
- Run `./gradlew :video-apps:app-batch:test` for batch changes.
