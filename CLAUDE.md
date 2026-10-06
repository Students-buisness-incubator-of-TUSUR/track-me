# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Track Me is an online service for automating and monitoring the work of project teams
(startups) and their curators ("trackers") within university acceleration programs.
It is a **microservices monorepo**: Java 25 / Spring Boot 3.5 backend services managed by a
multi-module Gradle build. The main React (CRA) SPA was extracted into its own repository
(`track-me-frontend`); only the SSO React app remains in this monorepo (built into the SSO jar).
The shared Gradle platform (BOM) and `commons` library were also extracted into their own repository
(`track-me-platform`) and are consumed as Maven artifacts from GitHub Packages.
Most domain comments and Liquibase changesets are written in Russian — match the existing
language when editing them.

## Build, test, lint

Gradle 9.7 via the wrapper (`gradle/wrapper/gradle-wrapper.properties`). All dependency
versions are pinned by the external **`net.trackme.platform:platform` BOM** from the
[`track-me-platform`](https://github.com/Students-buisness-incubator-of-TUSUR/track-me-platform)
repository (edit versions there, release a tag, then bump `trackmePlatformVersion` in the root
`gradle.properties` here — not in module builds). Root `gradle.properties` currently holds
`trackmePlatformVersion`, `trackme.java.version` and `trackme.errorprone.strict`.

**The build itself runs on Java 25.** The published `net.trackme.java-quality` plugin is compiled on
JDK 25, so its Gradle module metadata rejects older build JVMs (`Dependency requires at least JVM
runtime version 25`) — a 25 toolchain alone is not enough. `gradle/gradle-daemon-jvm.properties`
(daemon JVM criteria, regenerate with `./gradlew updateDaemonJvm --jvm-version=25`) makes Gradle
provision a JDK 25 daemon through the foojay resolver, so `./gradlew` also works from an older JVM;
CI (`setup-java` 25) and the Docker builder images already ship JDK 25.

Quality tooling (checkstyle, errorprone) is **not** wired in the root `build.gradle`. Every service
applies the external plugin **`net.trackme.java-quality`** (from `track-me-platform`, version pinned
per module build file) — that is what provides the `checkstyle*` tasks, overrides in
`config/checkstyle/checkstyle.xml` + `suppressions.xml`.

**GitHub Packages auth is required to build**: the BOM, `net.trackme.commons:commons` and the
`net.trackme.java-quality` plugin resolve from
`https://maven.pkg.github.com/Students-buisness-incubator-of-TUSUR/track-me-platform`, which needs any
valid GitHub token (PAT scope `read:packages`). Provide it as `gpr.user`/`gpr.key` in
`~/.gradle/gradle.properties` or via `GITHUB_ACTOR`/`GITHUB_TOKEN` env vars; docker-compose builds read
`GITHUB_TOKEN` from `.env` (passed as a BuildKit secret).

```bash
./gradlew build                       # build + test every module
./gradlew :backend:check              # compile + test + checkstyle for one module (use the Gradle project name)
./gradlew :backend:test               # tests only (JUnit 5; some use Testcontainers → Docker must be running)
./gradlew :backend:test --tests "net.trackme.backend.SomeTest"   # single test class/method
./gradlew checkstyleMain checkstyleTest        # all modules; or :backend:checkstyleMain for one
./gradlew :backend:jacocoTestReport            # coverage → build/reports/jacoco/test/jacocoTestReport.xml
```

Gradle project names (note paths differ from names): `:backend` (`apps/backend`),
`:trackme-gateway` (`apps/trackme-gateway`), `:trackme-sso` (`services/trackme-sso`),
`:meeting-service` (`services/meeting-service`), `:telegram-service` (`services/telegram-service`).

Checkstyle reports land at `<module>/build/reports/checkstyle/`.

### Frontend

The main React SPA now lives in a **separate repository** (`track-me-frontend`, extracted from the
former `apps/frontend`). Run it from there (`npm install && npm start`, dev server on :3000); it talks
to this backend only over HTTP via `REACT_APP_BACKEND_URI`. Locally the stack pulls its published
image (`ghcr.io/akarmanov2022/track-me/frontend:latest`) — see the `trackme-frontend` service in the
docker-compose files.

The only React app left in this monorepo is the SSO frontend (`services/trackme-sso/frontend`), built
**into the Spring jar** by Gradle (node plugin `com.github.node-gradle.node`): `:trackme-sso:processResources`
runs `npm run build` and copies output into `src/main/resources/{static,templates}`.

### Running the stack

```bash
docker compose up --build                                  # full stack; app at http://localhost (nginx)
docker compose -f docker-compose.dev.yaml up --watch       # dev: Compose Watch rebuilds on file change
./gradlew :backend:bootRun                                 # single service locally (default profile: development)
```

Containers run with `SPRING_PROFILES_ACTIVE=docker-local`. Local non-Docker runs default to the
`development` profile. Copy `.env.example` → `.env` first; see `docs/local-deployment.md` for the full
service/port table and required secrets (`JWT_SECRET`, `TRACKME_CLIENT_SECRET`, etc.).

The committed `docker-compose.yaml`/`docker-compose.dev.yaml` **build from source** and are for local
use only. The **prod** topology is `docker/docker-compose.prod.yaml` (image-pull by `${IMAGE_TAG}`, Spring
profile `production`, run as a separate project `-p track-me-prod --project-directory . -f docker/docker-compose.prod.yaml`
from `~/track-me-prod`). TLS and routing are done by the server's host-nginx
(`docker/nginx/host/trackme-prod.conf`, installed manually): `trackme.` / `api.trackme.` / `sso.trackme.startup-poligon.com`
→ frontend/gateway/sso published on `127.0.0.1:13000/18081/19000`. See `docs/prod-deployment.md` and `.env.prod.example`.

## Architecture

### Services and ports

| Module | Port | Role |
|---|---|---|
| `trackme-gateway` | 8081 | Spring Cloud Gateway (reactive/WebFlux). Single entry point, OAuth2 client, Redis sessions |
| `backend` | 8080 | Core domain: team cards, streams, NTI markets, readiness levels, meeting grades |
| `meeting-service` | 8082 | Meeting scheduling/tracking, reports |
| `trackme-sso` | 9000 | OAuth2 Authorization Server (OIDC issuer) — the identity provider |
| `telegram-service` | 8084 | Telegram bot; reacts to Kafka events |
| `frontend` | 3000 | Main React 19 SPA — **separate repo** (`track-me-frontend`); run locally as a pulled image |
| nginx | 80 | Reverse proxy in front of everything (`http://localhost`) |

Infra: one PostgreSQL, Redis (host port 6380), Kafka (9092/29092).

### Request flow & auth (important — touches every service)

`trackme-sso` is the OAuth2/OIDC **authorization server**. The **gateway** is the OAuth2 **client**:
it holds the Redis-backed user session and uses the `TokenRelay` filter to forward the access token
downstream. `backend` and `meeting-service` are **resource servers** that validate JWTs against the
SSO issuer (`SSO_URI`). Browser clients should call services through the gateway, never directly.

Gateway routes (with `StripPrefix=1`, so `/backend/api/...` → backend `/api/...`):
`/backend/**` → backend, `/meeting/**` → meeting-service, `/sso/**` → SSO.
Aggregated Swagger UI is exposed at the gateway: `http://localhost/swagger-ui`.

Beyond URL/JWT auth, **`backend` enforces domain-object-level permissions via Spring Security ACL**
(`commons` `AclService` + `PostgresJdbcMutableAclService`, ACL tables created by the
`initial-acl-schema` Liquibase changeset). When adding entities that need per-owner access control,
create ACLs through `AclService` and guard methods with method security — follow `TeamCardsServiceImpl`
/ `TeamCardsUseCase`.

### Asynchronous messaging (Kafka)

Services are decoupled via Kafka events (topic name = event type, no schema registry; JSON serializer).
Event payload records and producers/consumers live under each module's `messaging/` package (topic
names are also declared in each service's `KafkaTopicsConfiguration`).

- **meeting-service produces** → `meeting-created`, `meeting-updated`, `meeting-deleted`, `meeting-summary`.
- **backend produces** → `team-card-updated`, `team-card-summary`, `team-card-low-grade-summary`,
  `team-card-stream-added`, `team-card-stream-removed`, `meeting-not-happened`.
- **sso produces** → `user-updated`.

Consumers: backend `MeetingEventsListener` (meeting-*), meeting-service `TeamCardEventConsumer`
(team-card-*) and `SsoEventConsumer` (`user-updated`), telegram-service `TeamCardEventsListener` +
`MeetingNotHappenedEvent` listener, sso `TeamCardEventsListener`.

When you change an event record, update both the producer and the consumer copy (each service defines its
own local copy of the event class) so the JSON shape stays compatible.

### Persistence

Single Postgres database, **one schema per service** (`backend`, `meeting_service`, `telegram_service`,
`sso`). Schema is set via `spring.datasource.hikari.schema` + Liquibase `default-schema`/
`liquibase-schema`. JPA runs with `ddl-auto: validate` — **schema changes must be made through Liquibase
changelogs**, never by relying on Hibernate to create tables. Changelog roots:

- backend: `apps/backend/src/main/resources/db/changelog/db.changelog-master.yaml`
- meeting-service: `services/meeting-service/database/db.changelog-master.yaml`
- telegram-service: `services/telegram-service/database/db/changelog/db.changelog-master.yaml`
- sso: `services/trackme-sso/database/db.changelog.yaml` (releases under `release-X.Y.Z/`)

`database/init_schemas.sql` bootstrap-creates the schemas. JPA entities extend the base classes in
the `commons` library (`CoreEntity`, `BusinessEntity`, `VersionedBusinessEntity`).

### `commons` library (external, `net.trackme.commons:commons`)

Shared, non-Spring-Boot library depended on by backend/sso/meeting/telegram: base JPA entities,
a generic filtering/specification abstraction (`Filter`, `FilterRequest`, `OperationType`), and the
ACL support. It lives in the `track-me-platform` repository together with the platform BOM; both are
versioned and released together (git tag `vX.Y.Z` → GitHub Packages). To change it: edit there,
release, then bump `trackmePlatformVersion` in this repo's `gradle.properties`. For local iteration
use `./gradlew publishToMavenLocal` in `track-me-platform` and temporarily add `mavenLocal()` as the
first repository here (don't commit that). The `net.trackme.java-quality` plugin ships from the same repo.

## CI

`.github/workflows/pr-checks.yml` runs on push to `develop` and on PRs into `develop`, as a matrix over
**all 5 modules** (backend, trackme-gateway, trackme-sso, meeting-service, telegram-service): each runs
`./gradlew :<module>:check` and uploads its JaCoCo XML, then a `sonar` job reports everything to
SonarCloud, gated by `all-checks`. Run `./gradlew :<module>:check` locally before pushing. (The main
frontend has its own CI in the `track-me-frontend` repository.)

## Branches, builds & deployments

Two long-lived branches: **`develop`** (integration → dev, the default branch) and **`main`** (production → prod).

- `build-artifacts.yml` builds & pushes all 5 service images to GHCR on push to `develop`/`main` and on
  `v*.*.*` tags. Tags: always `:<sha>` + `:<ref_name>` (`:develop` / `:main` / `:vX.Y.Z`); `:latest` is
  moved **only from `develop`** so release tags / `main` never clobber the dev line.
- `deploy-dev.yml` auto-deploys `:<sha>` to the dev stand (`~/track-me-dev`) after a successful `develop` build.
- `deploy-prod.yml` ships `docker/docker-compose.prod.yaml` and `database/init_schemas.sql` to `~/track-me-prod`
  (paths preserved), patches `IMAGE_TAG` and `GOOGLE_*`/`YANDEX_*` in `.env`, and runs
  `docker compose -p track-me-prod --project-directory . -f docker/docker-compose.prod.yaml up -d` (healthchecked). Triggered by
  **GitHub Release published** (or `workflow_dispatch` with a `tag` for rollback), gated by the
  protected `production` environment (manual approval). **Prod images come only from `main`.**

Release flow and ops setup: `docs/prod-deployment.md`.

## Notes

- `README.md` is stale: it still claims PostgreSQL 13 and badges a
  `deploy-on-sbi.yml` workflow that no longer exists. Trust this file over the README.
- `docker/` also contains older `docker-compose.dev.yml` / `docker-compose.sbi.yml`; the live local
  topology is the root `docker-compose*.yaml` pair.
