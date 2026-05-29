# AGENTS.md

Governance for `video-core`.

## Scope

- Applies to `video-core/core-domain`, `video-core/core-usecase`, `video-core/core-port`, and `video-core/core-service`.
- Keep this layer technology-neutral. Infrastructure belongs in `video-adapters`.

## Dependency Direction

- `core-domain` must not depend on Spring, persistence, web, Redis, Kafka, MongoDB, QueryDSL, or adapter classes.
- `core-usecase` defines application-facing contracts and command objects. It may depend on domain objects, not adapters.
- `core-port` defines outbound interfaces used by services. It may depend on domain objects, not adapters.
- `core-service` implements use cases and orchestrates domain models through ports.
- Allowed direction is domain and contracts inward, implementations outward. Never make core depend on app or adapter modules.

## Domain Rules

- Put business invariants on domain objects when they are intrinsic to the model.
- Keep identifiers and value semantics consistent with existing domain classes.
- Do not add persistence annotations, request DTOs, response DTOs, or serialization-specific fields to domain models.
- Avoid framework exceptions in domain and use case contracts. Prefer existing error patterns.

## Use Case And Service Rules

- Services should depend on use case interfaces and port interfaces already present in this module.
- Add a new port only when a core service needs a new outbound capability.
- Keep transaction, cache, lock, and messaging mechanics outside core. Core can request the capability through a port.
- Keep command objects in `core-usecase` when they represent application input.

## Tests

- Prefer focused unit tests for domain and service behavior.
- Mock ports in service tests rather than reaching infrastructure.
- Run the smallest relevant module test first, such as `./gradlew :video-core:core-service:test`, then broaden if behavior crosses modules.
