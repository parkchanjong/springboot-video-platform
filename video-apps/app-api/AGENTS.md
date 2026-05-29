# AGENTS.md

Governance for `video-apps/app-api`.

## Scope

- Applies to REST controllers, DTOs, web configuration, argument resolvers, API docs, and controller tests.
- This module is an entry point. It should orchestrate use cases, not own business rules.

## Controller Rules

- Controllers should translate HTTP input into use case calls and translate use case output into response DTOs.
- Keep request and response shapes in `dto` unless an existing local pattern says otherwise.
- Do not inject repositories, Redis templates, Kafka clients, Feign clients, or adapter internals into controllers.
- Keep exception-to-response behavior in controller advice.

## Authentication Header

- Authenticated endpoints use `x-auth-key`, defined by `HeaderAttribute.X_AUTH_KEY`.
- Prefer the existing argument resolver and user/session use case path over parsing auth state in each controller.
- k6 scripts and API docs should use the same header spelling.

## API Docs Tests

- Controller tests extend `RestDocsTest` from `tests/api-docs`.
- Use the existing RestAssured MockMvc style and `mockController` setup.
- Add or update API documentation snippets when endpoint contracts change.
- Keep documented request fields, query parameters, headers, and response fields aligned with controller behavior.

## DTO Rules

- DTOs should be HTTP-facing only. Do not pass DTOs into core services when a command object already exists.
- Keep validation and conversion close to the endpoint unless a shared pattern already exists.

## Tests

- Run `./gradlew :video-apps:app-api:test` for API changes.
- If generated API docs are affected, inspect the snippets or generated docs produced by the test task.
