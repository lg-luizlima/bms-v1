# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build
./mvnw clean package
./mvnw clean package -DskipTests

# Run locally (uses PostgreSQL — start infra first)
./mvnw spring-boot:run

# Run with a specific profile
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"

# Start ALL local infrastructure — PostgreSQL:5433, Redis:6379, Kafka:9092, Schema
# Registry:8081, Debezium Connect:8083, Kafka UI:8080, plus observability (Grafana:3000,
# Jaeger:16686, Prometheus:9090) and admin UIs (pgAdmin:5051, RedisInsight:8001) — one compose
# file, no separate "basic infra" step, no login required on any of the web UIs.
docker compose -f debezium/debezium-docker-compose.yaml up -d
```

Server runs on port **8082** by default. Swagger UI available at `/swagger-ui.html`.

## Outbox + Debezium Flow

Current transition architecture:

1. `ms-vivopay-credit-consent-bms-v1` writes business data in PostgreSQL.
2. The same transaction writes an outbox row in `tb_outbox_events`.
3. Debezium captures outbox inserts via PostgreSQL WAL (CDC).
4. Debezium publishes to Kafka topic from `topic_name` field (Outbox Event Router).
5. `ms-vivopay-credit-consent-worker-v1` consumes and initially logs payload with `log.info`.

### Local CDC assets

All Debezium/Kafka Connect config lives in `debezium/` (see `debezium/README.md` — local env
setup + guide for configuring the equivalent connector in HML/prod; every new outbox-routed
topic, even one owned by another service like the worker, needs its connector config added
here too, since this repo owns provisioning the Debezium listener in every environment):

- `debezium/debezium-docker-compose.yaml` — local PostgreSQL + Kafka + Debezium Connect + Kafka UI.
- `debezium/debezium.json` — connector config for `public.tb_outbox_events` (this repo's own outbox).
- `debezium/debezium-worker-outbox.json` — connector config for `credit_consent_worker.tb_outbox_events` (`ms-vivopay-credit-consent-worker-v1`'s own outbox, → `vivopay.credit.engine.events.v1`).

### Event Hub parallel mode

Event Hub synchronous publish is kept temporarily in parallel, controlled by:

- `EVENTHUB_PARALLEL_PUBLISH_ENABLED` (default: `true`)

When this flag is `false`, app still writes outbox and Debezium remains the primary publication path.

## Observability

### Trace propagation through Debezium/CDC

`OutboxConsentEventAdapter.currentTraceParent()` captures the current span's W3C `traceparent`
(via injected `Tracer`/`Propagator`) into `tb_outbox_events.trace_context` in the same
transaction as the outbox write — in the adapter, so the core stays free of Micrometer. Debezium's outbox `EventRouter` promotes that column to a
Kafka header (`debezium/debezium.json`'s `transforms.outbox.table.fields.additional.placement`,
`trace_context:header:traceparent`). The worker's `KafkaConsumerConfig`/`KafkaProducerConfig`
have `ContainerProperties`/`KafkaTemplate.setObservationEnabled(true)` +
`setObservationRegistry(...)`, so Spring Kafka auto-extracts/injects that header — Kafka
consumption becomes a child span of the original HTTP request, not a new trace root. No manual
propagation code beyond the outbox column capture. `ApiExceptionHandler` uses the same `Tracer`
to put the real OTel trace id (not a random UUID) into every `ProblemDetailResponse.traceId`.

### SQL and Redis spans (PII policy: local/dev show values, hml/prod redact them)

- **SQL**: `net.ttddyy.observation:datasource-micrometer-spring-boot` (+ `-opentelemetry`,
  `2.2.1`) — auto-configured via the jar's own `AutoConfiguration.imports`, no bean wiring
  needed. Produces `db.query.text` on every JDBC span, but that text is **always**
  parametrized (`?`) since Hibernate exclusively uses `PreparedStatement` — the
  `jdbc.opentelemetry.analysis.sanitize.enabled` flag only strips literals from raw
  `Statement` SQL (verified against the library's bytecode; it never reaches this app's
  span attributes). **Actual bind parameter values** come from a separate mechanism:
  `jdbc.datasource-proxy.query.enable-logging` (`local`/`dev` only) turns on the underlying
  `datasource-proxy`'s SLF4J query logger, which logs the fully-bound SQL — those log lines
  carry `trace_id`/`span_id` via the same OTel logs bridge as everything else, so they're
  queryable alongside the span in Grafana even though they aren't span attributes.
  `application-{local,dev,hml,prod}.yml` template all of this `${VAR:default}` per profile —
  query-value logging on in local/dev, off (default) in hml/prod.
- **Redis**: Lettuce Observation is automatic in Boot 4.1
  (`LettuceObservationAutoConfiguration`, classpath-triggered once an `ObservationRegistry`
  bean exists — no property enables it) but only ever emits command name + peer address, never
  key/value. `RedisConsentCacheAdapter` — the only class that talks to Redis — wraps each command
  in a manual `Observation` (`redis.sync_status.write`, `redis.idempotency.read`,
  `redis.idempotency.write`; each the parent of Boot's automatic `GET`/`SET` span) that attaches
  `redis.key`/`redis.value` as span attributes only when
  `observability.pii.redis-values-enabled=true` (`ObservabilityPiiProperties` — `true` in
  local/dev, `false` in hml/prod). **The Java default is `false`; keep the hml/prod YAML defaults
  `false` too.** They read `true` for a while, which exported the CPF-bearing Redis key as a span
  attribute in production.

### Local observability stack

`debezium/debezium-docker-compose.yaml` (this repo) brings up `otel-collector` (4317 grpc / 4318 http),
`tempo` (3200), `jaeger` (16686), `loki` (3100), `prometheus` (9090, scrapes all 3 apps via
`host.docker.internal:{8082,8089,8084}/actuator/prometheus`), `grafana` (3000, anonymous
admin). Query a trace via Grafana's Tempo datasource, or directly:
`GET http://localhost:3200/api/traces/{trace_id}` (Tempo) or `http://localhost:16686` (Jaeger
UI, same OTLP data). Known gap: the collector's `metrics` pipeline
(`otel-collector-config.yaml`) only exports to `debug` — no real metrics backend over OTLP;
the Prometheus *scrape* of `/actuator/prometheus` is the only working metrics path today.

## Testing

```bash
./mvnw test                            # all unit tests (Mockito only, no Docker/DB needed)
./mvnw test -Dtest=ClassName            # single class
./mvnw test -Dtest=ClassName#methodName # single method
```

Most tests are plain Mockito (`@ExtendWith(MockitoExtension.class)`) with no Spring context and no
database. Two exceptions:
- `CreditCoreControllerTest` — `@WebMvcTest` slice. Boot 4 moved the annotation to
  `org.springframework.boot.webmvc.test.autoconfigure` (artifact `spring-boot-starter-webmvc-test`).
  The slice picks up lib-fintech-log's `MyFilter`, so the test supplies its two collaborators
  (`ObservationRegistry`, `SensitiveProperties` — the latter needs a **mutable** field set, its
  `init()` mutates it). There is no `com.fasterxml...ObjectMapper` bean: Boot 4 wires **Jackson 3**
  (`tools.jackson`) for HTTP while `JsonSerializer` uses Jackson 2 — both read the same
  `com.fasterxml.jackson.annotation` annotations, so `@JsonProperty` behaves identically either way.
- `HexagonalArchitectureTest` — ArchUnit; see "The core is framework-free" above.

Time is injected: `TimeConfig` exposes a `Clock` bean and the use cases take it, so tests assert on a
fixed instant instead of `Instant.now()`.

Test fixtures live in `src/test/java/br/com/tlf/dummies/`:
- `ConsentRequestDummies` — request DTOs, domain records and `CreateConsentCommand`; constants `CPF_PLAIN`, `BEARER_TOKEN`
- `CreditTermDummies` — `TermsCatalogEntry` fixtures (revokedTerm = hard update, softTerm = soft update)
- `CustomerConsentDummies` — `CustomerConsent` fixtures

## Architecture

Hexagonal (Ports & Adapters), Java 21, blocking JPA/Hibernate + Spring MVC (Tomcat) on the request path. Four top-level packages under `br.com.tlf`:

- **api** — REST layer: controllers, request/response DTOs (`api/rest/creditcore/dto/`), DTO↔domain mapper, header→identity resolver, exception handlers
- **core** — Domain records + rules (`core/domain/`), use cases (`core/application/usecase/`), ports (`core/port/in`, `core/port/out`)
- **infrastructure** — Outbound adapters: PostgreSQL JPA, Redis cache, Azure Event Hub, transactional outbox, transaction runner
- **shared** — Cross-cutting: `JsonSerializer`, `JwtTokenUtils`, `Clock` bean (`configuration/TimeConfig`), observability config

### The core is framework-free, and a test enforces it

`src/test/java/br/com/tlf/architecture/HexagonalArchitectureTest.java` (ArchUnit) fails the build if
`br.com.tlf.core..` gains a dependency on Jackson, Swagger, Jakarta Validation, Spring Data, Spring
transactions, Spring Web, JPA, Azure or Micrometer — and if `core.domain..` touches Spring at all.
**Read that test before adding a dependency to the core**: everything the core needs from the outside
goes through a `port/out` interface (`ConsentCachePort`, `TransactionRunner`, `ConsentEventOutbox`,
`EventHubPort`), and the adapter on the other side owns the framework.

That is why: the HTTP DTOs live in `api` and not in `port/in`; the inbound ports take
`CreateConsentCommand`/`PendingTermsQuery` records rather than raw HTTP headers; `StringRedisTemplate`
lives only in `RedisConsentCacheAdapter`; `TransactionTemplate` only in `SpringTransactionRunner`;
and `Tracer`/`Propagator` only in `OutboxConsentEventAdapter`.

### VO / DTO conventions

- **Domain types are immutable `record`s** under `core/domain/<aggregate>/` (`consent`, `terms`,
  `customer`), with the business rules as methods on them — `TermsCatalogEntry.isVigentAt(...)`,
  `.isNewerThan(...)`, `.expiryFrom(...)`, `TermsCatalog.latestVersionPerTermCode()`,
  `CustomerConsent.covers(term)`, `Cpf.isValid(...)`. There is no `vo` package and no `VO` suffix.
- **`DTO` suffix means HTTP contract** and only ever appears under `api/rest/`.
- **Wire formats for outbound integrations are separate records in `infrastructure`**, not domain
  types: `infrastructure/persistence/postgresql/outbox/contract/` (the outbox payload the worker
  consumes) and `infrastructure/eventhub/contract/`.

This repo has **no outbound HTTP client and no WebFlux dependency** — the only outbound integrations are Postgres (JPA), Redis (Lettuce), and Azure Event Hub. (The middleware repo, `ms-vivo-fintech-middleware-lending-core-credit-v1`, is the one with a `WebClientConfiguration`/`RestClient` outbound setup — don't confuse the two.)

### Database access — sync only, JPA, never R2DBC

This service is **100% synchronous/blocking** end-to-end: Spring MVC (Tomcat) on the
request path and **Spring Data JPA/Hibernate** for every database interaction. There is
no reactive stack anywhere in this codebase.

**Never introduce R2DBC, WebFlux, or any reactive database client** (`spring-boot-starter-data-r2dbc`,
`io.r2dbc.*`, `ReactiveTransactionManager`, `DatabaseClient`, `Mono`/`Flux` repositories, etc.) in this
repo. All persistence must go through:
- Entities in `infrastructure/persistence/postgresql/entity/` (suffix `JpaEntity`), mapped with JPA
  annotations (`@Entity`, `@Table`, `@Column`, ...).
- `JpaRepository` interfaces in `infrastructure/persistence/postgresql/jpa/`.
- Custom repository adapters in `infrastructure/persistence/postgresql/custom/` implementing the
  domain's `port/out` repository interfaces, using blocking `TransactionTemplate`/`@Transactional` calls.

If a future requirement seems to call for non-blocking DB access, treat that as a decision requiring
explicit product/architecture sign-off and a dedicated migration plan — do not add reactive
persistence code speculatively or as a partial/parallel implementation alongside the existing JPA
adapters.

### Request flow

`CreditCoreControllerOpen` → `CustomerIdResolver` + `CreditCoreApiMapper` (DTO → command) →
`CreateConsentPort`/`GetPendingTermsPort` → `CreateConsentUseCase`/`GetPendingTermsUseCase` →
out-port interfaces → infrastructure adapters → mapper (domain → DTO) → `ResponseDTO<T>` envelope

### REST Endpoints

Base path: `/credit-core/v1`

| Method | Path | Description | Auth Header | Response |
|--------|------|-------------|-------------|----------|
| GET | `/terms` | Get pending terms for a product | `authorization` | 200 `ActiveConsentResponseDTO` |
| POST | `/consents` | Create customer consent | `authorization` | 201 `ConsentResponseDTO` (wrapped in `ResponseDTO` envelope) |

**Possible error responses per endpoint** (all `ProblemDetailResponse`, documented as `@ApiResponse`s on
`CreditCoreControllerOpenApi` — see "OpenAPI documentation" below):

| Endpoint | Status | `DomainErrorCode`(s) |
|----------|--------|-----------------------|
| `GET /terms` | 400 | `MISSING_CUSTOMER_IDENTIFICATION`, `INVALID_CPF_PARAMETER` |
| `GET /terms` | 404 | `PRODUCT_NOT_FOUND` |
| `GET /terms` | 500 | `UNEXPECTED_ERROR` |
| `POST /consents` | 400 | `BAD_REQUEST` (bean validation / malformed JSON), `MISSING_CUSTOMER_IDENTIFICATION`, `INVALID_CPF_PARAMETER` |
| `POST /consents` | 422 | `INVALID_TERM`, `MANDATORY_TERM_NOT_ACCEPTED`, `TERM_NOT_FOUND`, `TERM_OUT_OF_VALIDITY` |
| `POST /consents` | 500 | `UNEXPECTED_ERROR` |

### Key port contracts

| Interface | Location | Adapter |
|-----------|----------|---------|
| `CreateConsentPort` | `core/port/in/` | `CreateConsentUseCase` |
| `GetPendingTermsPort` | `core/port/in/` | `GetPendingTermsUseCase` |
| `CustomerConsentRepository` | `core/port/out/customerconsent/` | `CustomerConsentCustomRepository` (JPA) |
| `TermsCatalogRepository` | `core/port/out/termscatalog/` | `TermsCatalogCustomRepository` (JPA) |
| `ConsentEventOutbox` | `core/port/out/outbox/` | `OutboxConsentEventAdapter` (JPA) |
| `ConsentCachePort` | `core/port/out/cache/` | `RedisConsentCacheAdapter` (Lettuce) |
| `TransactionRunner` | `core/port/out/transaction/` | `SpringTransactionRunner` (`TransactionTemplate`) |
| `EventHubPort` | `core/port/out/eventhub/` | `EventHubAdapter` (`EventHubProducerClient`) |

**CustomerConsentRepository methods:**
- `Map<String, CustomerConsent> findActiveConsentsByTermCode(String customerId, Collection<String> termCodes)` —
  **batched on purpose**: both use cases need the active consent for every term they inspect, and the
  previous per-term lookup issued one query per term.
- `CustomerConsent save(CustomerConsent consent)`

**TermsCatalogRepository methods:**
- `List<TermsCatalogEntry> findByIds(List<UUID> termIds)`
- `TermsCatalog findVigentTerms(String product)`

**One transaction per request, not per term.** `CreateConsentUseCase.registerConsents` collects every
not-yet-signed term first, then persists all of them (consent rows + outbox rows) inside a single
`TransactionRunner.runInTransaction(...)`. Doing it per term meant a failure on the second term left
the first one committed with its event already queued. `TransactionTemplate` rather than
`@Transactional` because the unit of work is a lambda handed over from the core — and a self-invoked
annotated private method is never intercepted by Spring's proxy-based AOP anyway.

**Event Hub publishes only after the terms validate.** The parallel publish used to run before
`resolveAndValidateTerms`, so a request that ended in a 422 had already emitted its event.

### Domain model

The service manages versioned **terms catalogs** per product (e.g. `EP_INSS`) and records customer
**consents** (accepted terms + audit signature).

**CPF is stored, cached and published in the clear — deliberately, and it must stay that way.** It is
the `tb_customer_consents.cpf` column, the customer id inside both Redis keys, the
`tb_outbox_events.aggregate_id` (which the Debezium outbox router turns into the **Kafka message
key**) and a field of the published payload. Hashing, HMAC-ing or tokenizing any of these is a
breaking change for downstream consumers and partitioning — do not introduce one without an explicit
migration decision.

Consent events are published to Azure Event Hub using the **outbox pattern** (`tb_outbox_events`,
consumed by Debezium CDC). Redis caches processing status during consent creation with key
`sync_status:{cpf}` (TTL via `SYNC_STATUS_TTL_REDIS`, default 86400s) and the idempotent
`POST /consents` response with key `post_consent_idempotency:{cpf}:{correlationId}` (TTL via
`CONSENT_IDEMPOTENCY_CHECK_TTL_REDIS`, default 60s). Both key formats live in
`infrastructure/cache/redis/RedisKeys` and are read by other services — keep them byte-identical.
Both TTLs come from `redis.ttl.*` via `RedisTtlProperties`; never reuse a business rule (a term's
validity period) as a cache TTL.

**JPA entities** (`infrastructure/persistence/postgresql/entity/`):
- `CustomerConsentJpaEntity` — cpf (64-char), termCode, termId (FK, plain UUID column, no relation), optIn, acceptedAt, expiresAt, auditDetails (`@JdbcTypeCode(SqlTypes.JSON)` → Postgres `jsonb`, mapped as `String`). The domain's `CustomerConsent` holds a `Signature` record instead; `CustomerConsentRepositoryMapper` serializes it to/from that column via `JsonSerializer`, so JSON encoding never reaches the core.
- `TermsCatalogJpaEntity` — product, termCode, version, isMandatory, validityDays, revokePreviousVersions, contentType, contentSummary, contentText, contentUrl, startAt, endAt
- `OutboxEventQueueJpaEntity` — aggregateType, aggregateId, topicName, payload (jsonb), createdAt (`@CreationTimestamp`)

### Schema management

Schema is owned exclusively by **Flyway** (`src/main/resources/db/migration/V{n}__{description}.sql`), not Hibernate `ddl-auto` — `spring.jpa.hibernate.ddl-auto` is set to `validate` everywhere (Hibernate checks the entities match the Flyway-managed schema at boot but never alters it). This is a deliberate choice: JPA is the runtime data-access layer, but schema evolution stays entirely in versioned SQL migration files, not annotation-driven auto-generation.

Schema lives in the database's default `public` schema — no dedicated schema, so `BMS_DATABASE_URL`/`DATABASE_URL` carry no `currentSchema` parameter and Flyway needs no `schemas`/`create-schemas` configuration. `debezium/debezium.json`'s `table.include.list` (`public.tb_outbox_events`) must stay in sync if this ever changes.

**⚠️ Every persistence change needs a new Flyway migration — never edit an applied one.** This covers both schema changes (new/altered columns, tables, indexes) and **data changes required by a new business rule** — e.g., adding a new product means a new `V{n}__seed_tb_terms_<product>.sql` inserting its `tb_terms` rows (see the `INSERT INTO tb_terms ... ON CONFLICT (id) DO NOTHING` block at the end of `V1__baseline.sql` for the pattern), not a manual `INSERT` run by hand against dev/hml/prod. Flyway checksums applied migrations; editing one that already ran in any environment breaks validation there. Add a new `V{n+1}__` file instead. The same rule applies to `ms-vivopay-credit-consent-worker-v1` (`src/main/resources/db/migration/`) for its own tables (`tb_processed_events`).

### Exception hierarchy

`BusinessException` is the base domain exception. Subclasses map to HTTP status via `ApiExceptionHandler` (RestControllerAdvice). All error responses follow `ProblemDetailResponse` format (errorCode, message, details, timestamp, traceId, errors list).

`ApiExceptionHandler` has **one** `@ExceptionHandler(BusinessException.class)` plus the
`MethodArgumentNotValid` override and a catch-all. Status and title come from
`DomainErrorRegistry.metadataFor(DomainErrorCode)` (`api/rest/config/exceptionhandler/DomainErrorRegistry.java`)
— a new domain error is one enum constant plus one registry entry, never another handler method. This
registry is also what the Swagger doc generator reads (see "OpenAPI documentation" below), so the
runtime error response and its documented example always agree.

| `DomainErrorCode` | Code | HTTP Status |
|-------------------|------|-------------|
| `BAD_REQUEST` | 4000 | 400 |
| `INVALID_CPF_PARAMETER` | 4002 | 400 |
| `MISSING_CUSTOMER_IDENTIFICATION` | 4003 | 400 |
| `INVALID_TOKEN` | 4004 | 400 |
| `PRODUCT_NOT_FOUND` | 4040 | 404 |
| `INVALID_TERM` | 4221 | 422 |
| `MANDATORY_TERM_NOT_ACCEPTED` | 4222 | 422 |
| `TERM_NOT_FOUND` | 4223 | 422 |
| `TERM_OUT_OF_VALIDITY` | 4224 | 422 |
| `UNEXPECTED_ERROR` | 5000 | 500 |

The numeric codes are part of the public contract — **append, never renumber**.

**Request-body validation is live.** `@Valid` on the `@RequestBody` was missing, which made every
`@NotEmpty`/`@NotNull`/`@NotBlank` on the DTOs inert and meant `{"acceptedTerms": [], "signature": null}`
returned 201 having saved nothing. It now returns 400, and the `@NotBlank` constraints on
`signature.ip`/`deviceId` are what enforce the "audit signature requires device data" rule (the
`MissingAuditDataException` that used to represent it was never actually thrown, and is gone).

**Never put the MDC stack trace outside a `try`-with-resources.** `MDC.putCloseable` is used so the
entry is scoped to the handler; Tomcat reuses request threads, and a plain `MDC.put` leaked the stack
trace into the next request served by that thread.

### Mappers

**Project standard: every object construction that is pure reshaping — copying fields from one or
more existing objects/params into a new one, no business decision, no side effect — is a
`@Mapper(componentModel = "spring")` method, never a `.builder()...build()` written inline in a
service, use case or repository.** Lombok + MapStruct annotation processors are both configured —
keep the `lombok-mapstruct-binding` dependency when adding new mappers.

The rule applies at every layer, not just the repository boundary: `CreateConsentUseCase` used to
build `CustomerConsent` and `ConsentRegisteredEvent` via inline `.builder()` calls; both are now one
`ConsentMapper` call each. The boundary that keeps this rule from swallowing all business logic: a
builder call that also makes a decision or reads a side effect is not mapping and stays inline —
`GetPendingTermsUseCase`'s `PendingTermsResult.builder()` computes `hasPendingMandatoryTerms` via
`.anyMatch(...)` (a decision), and `OutboxConsentEventAdapter`'s `OutboxEventQueueJpaEntity.builder()`
reads `currentTraceParent()` (a side effect) alongside constants — neither is a mapper candidate.

Every mapper sits **on a boundary**, and each boundary has exactly one:
- `CreditCoreApiMapper` (`api/rest/creditcore/mapper/`) — DTO ↔ domain, including `(ConsentRequestDTO, customerId, correlationId, channelId) → CreateConsentCommand`
- `TermsCatalogMapper` (`core/application/mapper/`) — `TermsCatalogEntry` → `PendingTerm`
- `ConsentMapper` (`core/application/mapper/`) — `(CreateConsentCommand, TermsCatalogEntry, AcceptedTerm, Instant) → CustomerConsent` and `(CustomerConsent, TermsCatalogEntry, Signature) → ConsentRegisteredEvent`; `expiresAt` keeps calling the domain's `TermsCatalogEntry.expiryFrom(...)` via a MapStruct `expression`, so the expiry rule itself still lives in the domain and the mapper only invokes it
- `CustomerConsentRepositoryMapper` (`infrastructure/.../mapper/`) — `CustomerConsent` ↔ `CustomerConsentJpaEntity`; an abstract class so it can `@Autowired` `JsonSerializer` for the `audit_details` jsonb column — that's the standard MapStruct pattern for a mapper with an injected collaborator, not an exception to the rule above
- `TermsCatalogRepositoryMapper` (`infrastructure/.../mapper/`) — `TermsCatalogJpaEntity` → `TermsCatalogEntry`
- `ConsentRegisteredPayloadMapper` (`infrastructure/.../outbox/`) — `ConsentRegisteredEvent` → outbox wire payload
- `ConsentRequestedEventMapper` (`infrastructure/eventhub/contract/`) — domain → Event Hub wire payload

There is no `Mappers.getMapper()` `INSTANCE` field anywhere: `componentModel = "spring"` is the single
instantiation strategy. Likewise `JsonSerializer` is the only JSON codec — do not `new ObjectMapper()`.

### OpenAPI documentation

Swagger UI (`/swagger-ui/index.html`, backed by `springdoc-openapi-starter-webmvc-ui`) is expected to be
self-sufficient: a consumer should be able to understand and try the API from it without reading the
source. `br/com/tlf/shared/configuration/OpenApiConfig.java` declares the API `Info` (title/description)
and the three `Server` entries (local, hml, prod) shown in the Swagger UI server dropdown.

**Project standard, going forward:**
- Every controller endpoint gets `@Operation(summary, description)` and `@Parameter(description, ...)` on
  each header/query param, plus `@ApiResponses` covering every HTTP status it can actually return —
  including every `DomainErrorCode` that maps to that status (see the error-response tables under
  "REST Endpoints" and "Exception hierarchy" above).
- **Error responses: description and example JSON are defined once, not on `@ApiResponse`.** A Java
  annotation attribute must be a compile-time constant, so `@ExampleObject`'s JSON can't be built from
  `DomainErrorCode` data directly — instead, stack one `@ApiErrorResponse(description = "...", codes =
  {DomainErrorCode.A, DomainErrorCode.B})` per HTTP status the endpoint can return
  (`api/rest/config/openapi/ApiErrorResponse.java`, `@Repeatable`), grouping every code that maps to that
  status under one human-authored sentence. `ApiErrorResponseCustomizer` (a springdoc
  `OperationCustomizer`, same package) reads these at doc-generation time, sets that description onto the
  matching `@ApiResponse(responseCode = ...)`, and builds one example per code from `DomainErrorRegistry`
  via `JsonSerializer.toJsonNode(...)` — the same registry `ApiExceptionHandler` reads for the real
  response, so the documented text can't drift from what the API actually returns. The `@ApiResponse`
  itself still needs a bare `content = @Content(mediaType = ..., schema =
  @Schema(implementation = ProblemDetailResponse.class))` — springdoc only registers `ProblemDetailResponse`
  in `#/components/schemas` because of that annotation (it's never a controller return type), so removing
  it would leave a dangling schema reference — but carries no `description` of its own, and the customizer
  throws `IllegalStateException` at startup if the matching `@ApiResponse` is missing, if two codes in one
  group resolve to different statuses, or if an example name collides with a hand-written one. A new
  `DomainErrorCode` needs one `DomainErrorRegistry` entry to become usable in docs at all, and each
  endpoint that can throw it still needs its own code added to the right `@ApiErrorResponse` group.
- **When a code's example can't be derived from the registry alone, override it — never hand-type
  JSON.** `POST /consents`'s `400` response needs an `errors[]` entry (`signature.deviceId must not be
  blank`) that's specific to that endpoint's DTO, not a generic per-`DomainErrorCode` fact — so
  `DomainErrorCode.BAD_REQUEST` is never listed in an `@ApiErrorResponse` group. Instead it gets
  `@ApiErrorExampleOverride(code = DomainErrorCode.BAD_REQUEST, factory = BadRequestValidationExample.class)`
  (`api/rest/config/openapi/ApiErrorExampleOverride.java`, `@Repeatable`), pointing at a small
  `Supplier<ProblemDetailResponse>` in `api/rest/creditcore/doc/` that builds the example from the same
  building blocks the real handler uses (`DomainErrorRegistry`, and
  `ApiExceptionHandler.MISSING_REQUIRED_FIELD_DETAILS` — a `public static final` constant shared between
  `ApiExceptionHandler.handleMethodArgumentNotValid` and this factory, so the `details` text can't drift
  either) — only the illustrative field name/message are hand-authored, since those are genuinely
  arbitrary per-DTO facts nothing can derive generically. `ApiErrorResponseCustomizer` processes both
  `@ApiErrorResponse` and `@ApiErrorExampleOverride` through the same content-building/duplicate-name-guard
  logic, so a hand-typed `@ExampleObject` should never be needed for an error response again.
- **Success (2xx) responses: the example envelope is generated from the same runtime call the controller
  makes, not hand-typed.** `ResponseDTO.message` used to carry a class-level `@Schema(example = ...)`,
  which is wrong by construction for a generic wrapper reused across endpoints (confirmed live: it showed
  one endpoint's message on the other's docs) — that attribute is gone now. Instead, each endpoint stacks
  one `@ApiSuccessExample(name = "...", factory = SomeExample.class)`
  (`api/rest/config/openapi/ApiSuccessExample.java`, `@Repeatable`) per realistic outcome it wants to show
  — `getActiveConsents` shows three (`NO_PENDING_TERMS`/`OPTIONAL_PENDING_TERM`/`MANDATORY_PENDING_TERM`,
  see `GetActiveConsentsNoPendingTermsExample`/`OptionalPendingTermExample`/`MandatoryPendingTermExample`
  in `api/rest/creditcore/doc/`), `createConsent` shows one (`SUCCESS`). Each factory is a small
  `Supplier<ResponseDTO<T>>` whose `get()` calls the *exact same* `ResponseDTO.ok(...)`/`.success(...)`
  factory and the *exact same* message constant (declared once on the `<Name>ControllerApi` interface,
  e.g. `GET_TERMS_SUCCESS_MESSAGE`, and referenced by both the factory and the real controller method)
  that the controller itself uses — only the sample `data` is illustrative; `status`/`message` are
  structurally guaranteed to match a real response. `ApiSuccessExampleCustomizer` instantiates each
  factory and attaches the results to the operation's one `2xx` response, throwing
  `IllegalStateException` if two examples on the same operation share a `name`.
- **The doc annotations live on a `<Name>ControllerApi` interface, not on the controller class.**
  `@Tag`, `@Operation`, `@ApiResponses`, `@ApiErrorResponse`/`@ApiErrorExampleOverride`,
  `@ApiSuccessExample`, `@Parameter`, and the Spring MVC mapping annotations
  (`@GetMapping`/`@PostMapping`/`@ResponseStatus`) all sit on the interface's method signatures. The
  controller class only declares `@RestController`, `@RequiredArgsConstructor`, the class-level
  `@RequestMapping(basePath)`, its constructor-injected fields, and `@Override` method bodies free of any
  repeated annotations — Spring MVC resolves the route/param binding and springdoc reads the docs straight
  off the interface method, so nothing is duplicated between the two files. This exists because a
  controller with more than one or two endpoints and full `@ApiResponses` coverage becomes mostly
  annotation noise around a handful of lines of actual wiring.
  `CreditCoreControllerOpenApi`/`CreditCoreControllerOpen` is the reference implementation.
- Every DTO field (request or response, including the shared `ResponseDTO`/`ProblemDetailResponse`
  envelopes) gets `@Schema(description, example, requiredMode)` — `requiredMode = REQUIRED` when the
  field carries `@NotNull`/`@NotBlank`/`@NotEmpty`, `NOT_REQUIRED` otherwise. `AcceptedTermDTO` is the
  reference implementation.
- Adding a new `DomainErrorCode` (see "Exception hierarchy" above) means updating every endpoint's
  `@ApiResponses`/`@ApiErrorResponse` groups that can throw it, not just the `DomainErrorRegistry` entry.

## Profiles & Environment

Default profile is `local`. Production profiles (`dev`, `hml`, `prod`) require:

| Variable | Purpose |
|----------|---------|
| `DATABASE_URL` | PostgreSQL JDBC URL — used by both JPA (runtime) and Flyway (bootstrap migration) |
| `DATABASE_USERNAME` | DB username |
| `DATABASE_PASSWORD` | DB password |
| `EUREKA_REGISTER_URL` | Service discovery |
| `SPRING_PROFILES_ACTIVE` | Active profile |
| `EVENTHUB_CONNECTION_STRING` | Azure Event Hub connection |
| `EVENTHUB_NAME` | Azure Event Hub name |
| `SYNC_STATUS_TTL_REDIS` | TTL (segundos) da chave Redis `sync_status:{cpf}` — default `86400` |
| `CONSENT_IDEMPOTENCY_CHECK_TTL_REDIS` | TTL (segundos) da chave Redis `post_consent_idempotency:{cpf}:{correlationId}` — default `60` |
| `REDIS_SSL` | Liga TLS no Lettuce (`spring.data.redis.ssl.enabled`) — default `true`. Azure Cache for Redis **exige** TLS na porta 6380 |
| `REDIS_TIMEOUT` | Timeout de comando Redis em ms — default `10000` |
| `REDIS_CONNECT_TIMEOUT` | Timeout de conexão/handshake Redis em ms — default `5000` |
| `OBSERVABILITY_PII_REDIS_VALUES` | Anexa `redis.key`/`redis.value` (que contêm o CPF) como atributos de span — default `true` em local/dev, **`false` em hml/prod** |
| `EVENTHUB_PARALLEL_PUBLISH_ENABLED` | Publicação síncrona em paralelo ao outbox — default `true` (`features.eventhub.parallel-publish-enabled`, lido por `EventHubPublishProperties`) |

Os TTLs de Redis são declarados nos `secrets` de cada ambiente
(`.azuredevops/config/{dev,hml,prod}/secrets.{yaml,yml}`) via marcadores `$(SYNC_STATUS_TTL_REDIS_DEV)` /
`$(CONSENT_IDEMPOTENCY_CHECK_TTL_REDIS_DEV)` (e equivalentes `_HML`/`_PROD`), portanto as variáveis precisam existir
na Secret Library do Azure DevOps. No perfil `local` valem os defaults do `application-local.yml`.

`REDIS_SSL`/`REDIS_TIMEOUT`/`REDIS_CONNECT_TIMEOUT` **não** são secrets — ficam em
`.azuredevops/config/{dev,hml,prod}/environments_variables.yml`. Atenção: a propriedade correta do Boot é
`spring.data.redis.ssl.enabled` (bloco aninhado `ssl:`); `sslEnabled` **não existe** em `RedisProperties` e é
silenciosamente ignorado pelo binder — foi exatamente essa a causa do `RedisCommandTimeoutException:
Connection initialization timed out after 1 minute(s)` em HML (cliente falando texto puro contra a porta TLS 6380).

### Redis é best-effort no `POST /consents`

Redis aqui é apenas cache (idempotência de curta janela e `sync_status`), nunca fonte de verdade.
`RedisConsentCacheAdapter` (única classe que fala com o Redis) captura
`org.springframework.dao.DataAccessException` (superclasse de `RedisConnectionFailureException`,
`RedisSystemException` e `QueryTimeoutException`), logam em `WARN` e seguem o fluxo: leitura vira cache miss,
escrita é descartada. As `Observation` continuam marcando o span como erro antes do catch. Trade-off aceito:
com o Redis fora, um replay do mesmo `correlationId` dentro da janela de 60s pode ser processado duas vezes —
preferível à indisponibilidade total do endpoint, e sinalizado no log de WARN.

## Key Dependencies

- **Spring Boot** (lib-starter-parent:4.1.2), **Spring MVC** (Tomcat) + **Spring Data JPA**, **Flyway** (schema), Spring Data Redis (blocking, Lettuce client)
- **Azure Event Hubs** — outbound event publishing (`EventHubProducerClient`, blocking)
- **MapStruct 1.5.3** + **Lombok** — VO/entity mapping
- **java-jwt 4.4.0** — JWT parsing in `JwtTokenUtils`. It only **decodes**: the signature is not
  verified here, so authenticity depends on the API gateway upstream.
- **SpringDoc OpenAPI 3** — Swagger UI
- **Netflix Eureka Client** — service discovery
- **Observability** (`spring-boot-starter-opentelemetry`) — Micrometer Tracing (OTel bridge) + OTLP trace/metrics export, fully managed by the Boot BOM (no explicit version pin needed, unlike `resilience4j`-style dependencies — confirmed against the `spring-boot-dependencies:4.1.0` POM). See README's "Observabilidade — OpenTelemetry" section (worker repo) for the full end-to-end design; this repo's role is capturing the current span's W3C traceparent into `tb_outbox_events.trace_context` so Debezium can promote it to a Kafka header. See "Observability" section below for SQL/Redis span instrumentation.
- **Logs bridge** (`io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.21.0-alpha`) — pinned explicitly (not BOM-managed); its transitive `opentelemetry-api-incubator` must be excluded and repinned to `1.62.0-alpha` to match the SDK version Boot brings, or startup fails with `NoSuchMethodError`. Installed via `br.com.tlf.shared.observability.OpenTelemetryLoggingConfig` + an `OpenTelemetry` appender added to the existing `logback-spring.xml`.
- **Metrics scrape** (`io.micrometer:micrometer-registry-prometheus`) — no `<version>` (managed by the `micrometer-bom` `spring-boot-dependencies` imports), exposes `/actuator/prometheus` alongside the existing OTLP metrics push.
- **SQL spans** (`net.ttddyy.observation:datasource-micrometer-spring-boot` + `-opentelemetry`, `2.2.1`) — Boot 4.1 has no native JDBC/Observation instrumentation; this library wraps the `DataSource` via `datasource-proxy` internally, auto-configured (no manual bean). See "Observability" section below.
