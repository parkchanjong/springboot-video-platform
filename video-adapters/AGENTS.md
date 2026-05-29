# AGENTS.md

Governance for `video-adapters`.

## Scope

- Applies to adapter implementations for JPA, MongoDB, Redis, Kafka, Feign, Redisson, and related configuration.
- This module implements `video-core:core-port` interfaces and translates between infrastructure models and domain models.

## Port Implementation Rules

- Adapter classes should implement core ports directly and keep infrastructure types inside the adapter boundary.
- Map JPA entities, Mongo documents, Redis payloads, Kafka messages, and Feign DTOs to domain objects before returning to core.
- Do not expose repository, template, Feign, Kafka, Redisson, QueryDSL, or persistence entity types through core ports.
- Keep serialization and deserialization localized to the adapter that owns the storage or transport.

## Persistence Boundaries

- Use JPA and QueryDSL for MySQL relational data such as videos, channels, users, subscriptions, coupons, and coupon policies.
- Use MongoDB repositories and documents for comment hierarchy behavior.
- Use Redis for cache, counters, sessions, like or block sets, and coupon state where existing keys already define ownership.
- Use Kafka producers and consumers for new-video notification messaging.
- Use Feign clients for external book APIs and keep provider-specific DTOs under their provider package.
- Use Redisson only through the distributed lock adapter and the corresponding core port.

## QueryDSL And Repository Rules

- Keep QueryDSL configuration under `jpa/config` and custom query code under the owning JPA package.
- Do not move query composition into services or controllers.
- Preserve existing repository naming and package patterns.

## Tests

- Adapter tests should verify mapping, cache keys, repository delegation, serialization, and port behavior.
- Mock infrastructure clients when the test is not explicitly an integration test.
- Run `./gradlew :video-adapters:test` for adapter changes.
