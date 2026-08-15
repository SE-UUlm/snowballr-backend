# AGENTS

## Overview

This repository contains the Kotlin gRPC backend and Python-based fetcher plugins. Prefer referencing the wiki and
existing docs over restating them here. If you must summarize, keep it short and point to the canonical page.

### SnowballR repositories

- Organization: https://github.com/SE-UUlm
- SnowballR (umbrella repo): https://github.com/SE-UUlm/snowballr
- SnowballR API: https://github.com/SE-UUlm/snowballr-api
- SnowballR Backend: https://github.com/SE-UUlm/snowballr-backend
- SnowballR Frontend: https://github.com/SE-UUlm/snowballr-frontend
- SnowballR CI: https://github.com/SE-UUlm/snowballr-ci
- SnowballR Mock Backend: https://github.com/SE-UUlm/snowballr-mock-backend
- SnowballR Backend (legacy): https://github.com/SE-UUlm/snowballr-backend-old

### Canonical documentation and what each covers

- README.md — repository overview and pointers
- wiki/Getting-Started.md — local setup, Docker, build/run, IDE instructions
- wiki/Configuration.md — environment variables, profiles, JWT key generation, auth bypass
- wiki/Architecture.md — request flow and layer responsibilities
- wiki/Contributing.md — project layout and implementation patterns by layer
- wiki/Logging.md — log level rules, what must never be logged, layer responsibilities, correlation IDs
- wiki/Testing.md — unit/integration testing conventions and reports
- wiki/Fetcher.md — fetcher plugin system, security warning, contract
- wiki/Tools.md — dev tools, including fetcher CLI reference

## Structure

```
.
├── src/
│   ├── main/
│   │   ├── kotlin/              # production code (table, repository, access, service, validation, grpc)
│   │   └── resources/           # runtime resources (including fetcher plugin libs)
│   └── test/
│       └── kotlin/              # tests mirroring production layout
├── tools/
│   └── fetcher-cli/             # CLI tool for directly invoking fetcher plugins
├── plugins/
│   └── fetchers/                # local fetcher plugins and shared libs
├── .github/workflows/           # CI, release, and wiki automation
├── scripts/                     # CI and wiki helper scripts
├── docker-compose.yml           # local dev stack and profiles
├── requirements.txt             # Python dependencies for fetcher plugins
└── wiki/                        # canonical documentation
```

## Where to look

| Task                            | Location                                                                 | Notes                                            |
|---------------------------------|--------------------------------------------------------------------------|--------------------------------------------------|
| Project overview                | README.md                                                                | High-level pointers.                             |
| Local setup / Docker            | wiki/Getting-Started.md                                                  | Includes compose profiles.                       |
| Configuration                   | wiki/Configuration.md, .env.example                                      | Profiles and env vars.                           |
| Architecture                    | wiki/Architecture.md                                                     | Request flow and layer responsibilities.         |
| Project layout / layer patterns | wiki/Contributing.md                                                     | Source of truth.                                 |
| Fetcher orchestration progress  | wiki/Contributing.md#fetcher-orchestration-progress                      | Implementation checklist.                        |
| Logging conventions             | wiki/Logging.md                                                          | Levels, layer ownership, MDC correlation.        |
| Logging setup                   | src/main/resources/logback.xml, context/RequestContext.kt                | Pattern and MDC mirroring.                       |
| Testing conventions             | wiki/Testing.md                                                          | Unit/integration tests, reports.                 |
| Fetcher contract                | wiki/Fetcher.md                                                          | Security warning and invocation protocol.        |
| Fetcher CLI                     | tools/fetcher-cli/cli.py, wiki/Tools.md                                  | Direct plugin invocation for dev/testing.        |
| Fetcher orchestrator            | src/main/kotlin/se/uulm/snowballr/backend/fetcher/FetcherOrchestrator.kt | Job queue logic.                                 |
| Fetcher orchestrator tests      | src/test/kotlin/se/uulm/snowballr/backend/fetcher/orchestrator           | Unit tests.                                      |
| Orchestrator wiring/startup     | src/main/kotlin/se/uulm/snowballr/backend/Module.kt, Main.kt             | Koin wiring, start/stop.                         |
| Architecture tests              | src/test/kotlin/se/uulm/snowballr/backend/arch/LayerArchitectureTest.kt  | Layer constraints incl. fetcher.                 |
| Build config                    | build.gradle.kts                                                         | apiVersion, Gradle tasks, fetcher deps.          |
| Python tool config              | pyproject.toml                                                           | ruff (lint/format) and ty (type-check) settings. |
| Docker images                   | Dockerfile, Dockerfile.proxy                                             | Backend and proxy containers.                    |
| CI workflows                    | .github/workflows                                                        | Lint, tests, docker, wiki publish.               |

## Architecture and patterns

- **Layered flow:** Authentication → Validation → gRPC server → Service → AccessChecker → Repository → DB
  (wiki/Architecture.md).
- **Change workflow (layers):** Table → Repository → Access rules/checkers → Service → Validation
  (wiki/Contributing.md).
- **Fetcher orchestrator:** In-process queue for fetcher jobs. It is started in Main.kt and can only be started once
  (see FetcherOrchestrator.kt and related tests).

## Logging

Full rules in wiki/Logging.md. The points most easily got wrong:

- **PRODUCTION runs at DEBUG.** Everything except TRACE is visible in production, so the level does not decide whether
  a line is seen but how it is treated: act on it (WARN/ERROR), record a state change (INFO), or diagnose one request
  (DEBUG). Levels must stay separable, since filtering to INFO and above is how the audit trail is read.
- **Levels:** ERROR only when an operator must act (broken invariant, unhandled exception). WARN for expected but
  noteworthy events, including security-relevant rejections such as failed logins. INFO for state changes. DEBUG for
  per-request diagnostics. TRACE is the only level not enabled in production.
- **Layer ownership:** services log business events; repositories log only batch/maintenance row counts; interceptors
  log the request lifecycle. Never log the same event at two layers.
- **Never log** passwords, JWTs, verification/invitation tokens (they are bearer credentials), or raw request payloads.
  Validated email addresses are fine.
- **Correlation:** requestId and userId are mirrored into the SLF4J MDC by RequestContext. A plain MDC.put does not
  survive dispatcher hops in coroutine code — use a ThreadContextElement.

## Fetcher plugins

- Fetcher plugins run on the same system as the backend; treat third-party fetchers as untrusted by default and only run
  fetchers you trust (wiki/Fetcher.md).
- Fetchers live under plugins/fetchers by default. Use PLUGIN_DIRECTORY to relocate (wiki/Configuration.md).
- The invocation contract and stdin JSON payloads are specified in wiki/Fetcher.md.
- Base Python deps are in requirements.txt; use uv to sync (commands below).

## Configuration

- Use .env.example as a baseline and wiki/Configuration.md for required variables and profile defaults.
- Profiles: PRODUCTION, DEVELOPMENT, TESTING; use PROFILE to switch (wiki/Configuration.md).

## API versioning

- build.gradle.kts controls the SnowballR API proto version (apiVersion).
- Follow wiki/Contributing.md#use-another-api-version when changing it.

## Boundaries

- Always do: prefer wiki references for process guidance; keep changes focused to the requested scope; follow existing
  layer patterns from wiki/Contributing.md.
- Ask first: apiVersion changes, database schema or migration changes, CI workflow edits, Docker image or compose
  profile
  changes, dependency additions, changes to fetcher contract or plugin loading rules.
- Never do: commit secrets or real credentials; edit generated build artifacts under build/; modify db-data/; change
  .env
  with production values; bypass security warnings for fetchers.

## Commands (run from repo root)

### Build and run

- Build jar: `./gradlew shadowJar` — produces build/libs/snowballr-backend-<version>.jar (wiki/Getting-Started.md)
- Run locally: `./gradlew run` — prepares .venv and syncs requirements.txt (wiki/Getting-Started.md)
- Run jar: `java -jar build/libs/snowballr-backend-<version>.jar` (wiki/Getting-Started.md)

### Lint and format

- Lint (Kotlin): `./gradlew lint` (wiki/Contributing.md)
- Format (Kotlin): `./gradlew format` (wiki/Contributing.md)
- Format (Python): `uvx ruff format .` (wiki/Contributing.md)
- Lint (Python): `uvx ruff check .` (wiki/Contributing.md)
- Type-check (Python): `uvx ty check` — requires `.venv` with deps synced (wiki/Contributing.md)

### Tests

- Unit tests: `./gradlew test` (wiki/Testing.md)
- Integration tests: `./gradlew integrationTest` (wiki/Testing.md)
- Reports: build/testReportHtml, build/integrationTestReportHtml, build/coverageHtml (wiki/Testing.md)

### Docker

- Default stack: `docker compose up` (wiki/Getting-Started.md)
- Profiles:
    - `docker compose --profile db-only up`
    - `docker compose --profile registry up`
    - `docker compose --profile proxy-only up`
    - `docker compose --profile backend-only up`
      (meaning described in wiki/Getting-Started.md and docker-compose.yml)

### Fetcher dependencies

- `uv venv .venv`
- `uv pip install --python .venv/bin/python3 -r requirements.txt`
  (wiki/Getting-Started.md, wiki/Fetcher.md)

### Fetcher CLI

- Run subcommand: `uv run ./tools/fetcher-cli/cli.py <subcommand> <args>` (wiki/Tools.md)
- Init config: `uv run ./tools/fetcher-cli/cli.py init-config` — creates `tools/fetcher-cli/config.json` with per-fetcher config stubs
- Subcommands: `list`, `info`, `search`, `forwards`, `backwards`
- Output saved to `tools/fetcher-cli/output/`

## Style, checks, and tests

- **Style:** Follow .editorconfig (4-space indent, max line length 120 for Kotlin). Python line length is 100 (pyproject.toml).
- **Checks (Kotlin):** Detekt is the linter; keep code consistent with detekt.yml.
- **Checks (Python):** ruff for formatting and linting; ty for type-checking. Config in pyproject.toml.
- **Tests:** Follow wiki/Testing.md conventions (when-then naming, nested classes).
- **Logging:** Follow wiki/Logging.md; always use the lambda form (`logger.info { ... }`) so the message is only built
  when the level is enabled.

Example test naming:

```kotlin
@Test
fun `When input is invalid, then validation fails`() {
    // ...
}
```

## Issues

- Use .github/ISSUE_TEMPLATE to pick the right template.

## PRs

- Use .github/pull_request_template.md for required sections.

## Git and CI conventions

- PRs to develop must keep a linear history (see .github/workflows/git_conventions.yml).
- CI workflows run lint, unit tests, integration tests, and Docker builds (.github/workflows).
- Python code quality (ruff format/check + ty type-check) runs in CI on changes to Python files or pyproject.toml (.github/workflows/code_quality_checks.yml).
- Wiki linting and publish are handled in .github/workflows/wiki.yml.

## Conventional commits

Recent commit messages follow Conventional Commits with a short type prefix and optional scope. Common types in this
repo
include: feat, fix, refactor, test, docs, chore, ci. Use lowercase types and keep the subject imperative and concise.

Examples from recent history:

- feat: add repo calls for adding forward and backward references
- fix: normalize plugin directory path resolution and defaults
- refactor: move custom ColumnType implementations to separate directory
- test: check for specific SQL exception
- docs: add comments in Dockerfile
- ci: only upload coverage if tests were successful
- chore(deps): bump idna in the pip group across 1 directory
