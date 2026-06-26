# gRPC → REST + Spring Boot Migration Plan

Migration of the backend from gRPC to a REST API on **Spring Boot MVC**, replacing
protobuf-generated types with our own Kotlin types and a code-first generated TS client.

**Strategy:** strangler-fig, single JVM / two ports. gRPC remains the production transport
until a single final cutover. The enabling idea is a **domain-model seam** — make the service
layer transport-neutral *first*, so both gRPC (now) and REST (later) are thin mappers over the
same Kotlin types. No protobuf hop survives in the REST path, so nothing ossifies.

> **Biggest risk — Phase 0 discipline.** If REST controllers are written before the service
> layer is genuinely proto-free, the double-mapping ossifies and the whole benefit is lost.
> **Phase 0 must land first.**

## Locked decisions

| Area                | Decision                                                                                                                                                 |
|---------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| Transport/framework | Spring **MVC** (not WebFlux — persistence is blocking JDBC via Exposed); keep `suspend` services                                                         |
| Contract            | **Code-first**: springdoc emits `openapi.json` (CI drift-check) → openapi-generator TS npm client published from backend CI; retire `snowballr-api` repo |
| DTOs                | Kotlin data classes + Jackson; `XDto` / `CreateXRequest` / `UpdateXRequest`                                                                              |
| Updates             | **Full-replace** for simple resources, **intent endpoints** for special cases (status, settings); field masks removed                                    |
| Coexistence         | Koin keeps owning services; Spring owns only the web layer; controllers are `KoinComponent`s; **Koin→Spring DI is the LAST step**                        |
| Enums               | Native Kotlin enums replacing proto enums (no prod data → free swap; currently persisted by ordinal)                                                     |
| Identity            | Neutral `RequestContext` coroutine-context element replacing `io.grpc.Context`; Spring Security only at the edge filter                                  |
| Validation          | Existing Arrow validators stay for gRPC; new REST DTOs get Bean Validation additively                                                                    |
| Exceptions          | Keep `SnowballRException` hierarchy; add `@RestControllerAdvice` + `ProblemDetail`                                                                       |
| Tests               | Per-domain lockstep migration of the proto-coupled tests; thin `@WebMvcTest` slices, small and later                                                     |
| Cutover             | Continuous frontend integration against staging; single production switch; `/api/v1` + semver from endpoint one                                          |

Domains (the 12 services, used as the per-domain unit of work below):
`User`, `Project`, `Paper`, `Review`, `Criterion`, `ProjectPaper`, `ProjectMember`,
`Invitation`, `ReadingList`, `Authentication`, `Fetcher`, `Export`.

---

## Phase 0 — Neutralize the service layer (no REST yet; gRPC still the only transport)

Goal: services speak only domain/command Kotlin types; protobuf lives **only** in the gRPC
mapping layer. Work proceeds **per-domain**, tests migrated in lockstep (same PR).

### 0.1 Native enums (replace proto enums)
- [ ] Define native Kotlin enums for `ProjectStatus`, `SnowballingType`, `ReviewDecisionMatrix`, `UserRole`, `UserStatus`, `MemberRole` (+ any others)
- [ ] Update `table/` Exposed `enumeration<>` columns to native enums
- [ ] Update `table/columntypes/` if affected
- [ ] Update repositories to use native enums
- [ ] Update `model/dto/` domain classes to use native enums
- [ ] Update validators and business logic that branch on enum values
- [ ] Add proto↔native enum mapping in the gRPC layer
- [ ] Migrate affected tests in lockstep
- [ ] Recreate/drop dev schema (no prod data)

### 0.2 Neutral `RequestContext`
- [ ] Create `RequestContext` as a Kotlin coroutine-context element (transport-agnostic package): `userId`, `authStatus`, mutable cookie sink
- [ ] Provide `RequestContext.current()` accessor + `ThreadContextElement` bridging
- [ ] gRPC `AuthenticationInterceptor` populates `RequestContext` instead of `io.grpc.Context`
- [ ] Swap `ServiceHelper.withUser` / `GrpcContext.getUserIdFromContext()` → `RequestContext.current()`
- [ ] Move cookie-sink writes (`AuthenticationManager` `COOKIES_TO_SET`) onto `RequestContext`; gRPC interceptor drains it to metadata
- [ ] Remove `io.grpc.Context` usage from the service/auth layer
- [ ] Migrate affected tests

### 0.3 Domain command types + transport-neutral service signatures (per-domain)
For each domain — `[ ] User · [ ] Project · [ ] Paper · [ ] Review · [ ] Criterion · [ ] ProjectPaper · [ ] ProjectMember · [ ] Invitation · [ ] ReadingList · [ ] Authentication · [ ] Fetcher · [ ] Export`:
- [ ] Introduce input **command** types (e.g. `CreateXCommand`, `UpdateXCommand`) replacing proto `.Create`/`.Update`
- [ ] Change service return types from `GrpcX` to domain types; hoist `.toGrpcX()` **up** into the gRPC server/mapping layer
- [ ] Move `toGrpc*` extensions out of `model/dto/` into the gRPC layer
- [ ] gRPC server method maps proto ↔ domain/command and calls the neutral service
- [ ] Migrate that domain's tests proto→domain in the same PR

**Phase 0 exit criteria:** no `snowballr.*` / `com.google.protobuf` imports outside the `grpc/` package; full suite green.

---

## Phase 1 — Add REST alongside gRPC (gRPC still in production)

### 1.1 Spring Boot wiring
- [ ] Add Spring Boot MVC to `build.gradle.kts`
- [ ] Spring Boot becomes entrypoint; start grpc-netty as a `CommandLineRunner`/bean during coexistence
- [ ] Embedded Tomcat on a second port next to grpc-netty; reconcile Netty versions
- [ ] Bridge: controllers resolve existing `I*Service` singletons from Koin (`KoinComponent`)
- [ ] Add `springdoc-openapi`; emit `openapi.json` at build; CI drift-check vs annotations
- [ ] CI: generate + publish TS npm client (semver); keep `snowballr-api` publishing until retired

### 1.2 Cross-cutting web layer
- [ ] Spring Security filter: parse cookie → validate JWT → populate `RequestContext` (401/403)
- [ ] `@RestControllerAdvice` mapping `SnowballRException` → HTTP via `ProblemDetail`
- [ ] Versioned base path `/api/v1`

### 1.3 Controllers per-domain (separate package)
For each domain — `[ ] User · [ ] Project · [ ] Paper · [ ] Review · [ ] Criterion · [ ] ProjectPaper · [ ] ProjectMember · [ ] Invitation · [ ] ReadingList · [ ] Authentication · [ ] Fetcher · [ ] Export`:
- [ ] REST DTOs (Jackson + OpenAPI annotations + Bean Validation)
- [ ] DTO ↔ domain mappers
- [ ] Controller endpoints: full-replace for simple updates, **intent endpoints** for special cases
- [ ] Thin `@WebMvcTest` slice (mapping, status, validation, auth) — small, added incrementally
- [ ] Frontend integrates this domain against staging via the generated client

### 1.4 Special cases
- [ ] Binary endpoints (`getPaperPdf`, `exportProject`): `ResponseEntity<ByteArray>`/`Resource` with correct content-type (not JSON)
- [ ] Health/metrics via Spring Actuator

---

## Phase 2 — Cutover & cleanup

- [ ] All domains have REST equivalents + frontend integrated on staging
- [ ] One or two `@SpringBootTest` smoke tests per domain
- [ ] OpenAPI-driven fuzzing/resilience pass (e.g. Schemathesis) against the running REST API
- [ ] **Single production switch** to REST
- [ ] Drop the Envoy proxy
- [ ] Delete gRPC server, interceptors, `.proto` consumption, reflection/health
- [ ] Retire the `snowballr-api` repo (publishing already relocated to backend CI)

## Phase 3 — DI migration (last)

- [ ] Migrate services from Koin to Spring DI
- [ ] Remove Koin
- [ ] Re-home `SchedulerManager` / `FetcherOrchestrator` lifecycle under Spring

---

> Per-endpoint definition of done (keeps the conversion layer shrinking, never growing):
> frontend calls REST for it **and** the gRPC method is removed **and** no protobuf remains in that path.
