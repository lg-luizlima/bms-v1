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

# Start local infrastructure (PostgreSQL:5433, Redis:6379, Kafka:9092)
cd docker-compose && docker-compose up -d

# Start CDC stack for Outbox + Debezium (PostgreSQL:5433, Kafka:9092, Connect:8083, Kafka UI:8080)
docker compose -f debezium-docker-compose.yaml up -d
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

- `debezium-docker-compose.yaml` — local PostgreSQL + Kafka + Debezium Connect + Kafka UI.
- `debezium.json` — connector config for `credit_consent.tb_outbox_events`.

### Event Hub parallel mode

Event Hub synchronous publish is kept temporarily in parallel, controlled by:

- `EVENTHUB_PARALLEL_PUBLISH_ENABLED` (default: `true`)

When this flag is `false`, app still writes outbox and Debezium remains the primary publication path.

## Observability

### Trace propagation through Debezium/CDC

`CreditCoreServiceImpl.currentTraceParent()` captures the current span's W3C `traceparent`
(via injected `Tracer`/`Propagator`) into `tb_outbox_events.trace_context` in the same
transaction as the outbox write. Debezium's outbox `EventRouter` promotes that column to a
Kafka header (`debezium.json`'s `transforms.outbox.table.fields.additional.placement`,
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
  key/value. `CreditCoreServiceImpl.writeSyncStatus(...)` wraps the single Redis write in a
  manual `Observation` (`redis.sync_status.write`, parent of Boot's automatic `SET` command
  span) that attaches `redis.key`/`redis.value` as span attributes only when
  `observability.pii.redis-values-enabled=true` (`ObservabilityPiiProperties` — `true` in
  local/dev, `false` in hml/prod).

### Local observability stack

`debezium-docker-compose.yaml` (this repo) brings up `otel-collector` (4317 grpc / 4318 http),
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

Tests use Mockito (`@ExtendWith(MockitoExtension.class)`) — no Spring context loaded, no H2. Test fixtures live in `src/test/java/br/com/tlf/dummies/`:
- `ConsentRequestDummies` — request/response DTOs and VOs; constants `CPF_PLAIN`, `BEARER_TOKEN`
- `CreditTermDummies` — `TermsCatalogVO` fixtures (revokedTerm, softTerm)
- `CustomerConsentDummies` — `CustomerConsentVO` fixtures

## Architecture

Hexagonal (Ports & Adapters), Java 21, blocking JPA/Hibernate + Spring MVC (Tomcat) on the request path. Four top-level packages under `br.com.tlf`:

- **api** — REST layer: controllers, request/response DTOs, exception handlers
- **core** — Domain: port interfaces (`port/in`, `port/out`), application services, domain VOs, exceptions
- **infrastructure** — Outbound adapters: PostgreSQL JPA, Azure Event Hub, outbox pattern
- **shared** — Cross-cutting: utilities (`HmacUtils`, `JwtTokenUtils`, `MathUtils`), config beans (`observability/OpenTelemetryLoggingConfig`, `observability/ObservabilityPiiProperties`)

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

`CreditCoreController` → `CreditCorePortIn` → `CreditCoreServiceImpl` → out-port interfaces → infrastructure adapters

### REST Endpoints

Base path: `/credit-core/v1`

| Method | Path | Description | Auth Header | Response |
|--------|------|-------------|-------------|----------|
| GET | `/terms` | Get pending terms for a product | `authorization` | 200 `ActiveConsentResponseDTO` |
| POST | `/consents` | Create customer consent | `authorization` | 201 `ConsentResponseDTO` (wrapped in `ResponseDTO` envelope) |

### Key port contracts

| Interface | Location | Adapter |
|-----------|----------|---------|
| `CreditCorePortIn` | `core/port/in/creditcore/` | `CreditCoreServiceImpl` |
| `CustomerConsentRepository` | `core/port/out/customerconsent/` | `CustomerConsentCustomRepository` (JPA) |
| `TermsCatalogRepository` | `core/port/out/termscatalog/` | `TermsCatalogCustomRepository` (JPA) |
| `OutboxEventQueueRepository` | `core/port/out/outbox/` | `OutboxEventQueueCustomRepository` (JPA) |
| `EventHubPort` | `core/port/out/eventhub/` | `EventHubAdapter` (`EventHubProducerClient`) |

**CustomerConsentRepository methods:**
- `CustomerConsentVO getActiveCustomerConsent(String cpf, String termCode)`
- `CustomerConsentVO saveConsent(CustomerConsentVO consent)`

**TermsCatalogRepository methods:**
- `List<TermsCatalogVO> findLatestActiveByProduct(String product)`

The consent + outbox write is wrapped in a single transaction via `TransactionTemplate` (`CreditCoreServiceImpl.saveConsentAtomically`), not `@Transactional` on a private method — self-invoked private methods are never intercepted by Spring's proxy-based AOP, so that annotation would silently be a no-op regardless of blocking vs. reactive.

### Domain model

The service manages versioned **terms catalogs** per product (e.g. `EP_INSS`) and records customer **consents** (accepted terms + audit signature). cpf are not hashed anywhere (repository lookups, Redis keys) lib-fintech-logs leads with de security problem . Consent events are published to Azure Event Hub using the **outbox pattern** (`tb_outbox_events` table, consumed by Debezium CDC). Redis (`StringRedisTemplate`) caches processing status during consent creation with key `sync_status:{cpf}`.

**JPA entities** (`infrastructure/persistence/postgresql/entity/`):
- `CustomerConsentJpaEntity` — cpf (64-char), termCode, termId (FK, plain UUID column, no relation), optIn, acceptedAt, expiresAt, auditDetails (`@JdbcTypeCode(SqlTypes.JSON)` → Postgres `jsonb`, mapped as `String`)
- `TermsCatalogJpaEntity` — product, termCode, version, isMandatory, validityDays, revokePreviousVersions, contentType, contentSummary, contentText, contentUrl, startAt, endAt
- `OutboxEventQueueJpaEntity` — aggregateType, aggregateId, topicName, payload (jsonb), createdAt (`@CreationTimestamp`)

### Schema management

Schema is owned exclusively by **Flyway** (`src/main/resources/db/migration/V{n}__{description}.sql`), not Hibernate `ddl-auto` — `spring.jpa.hibernate.ddl-auto` is set to `validate` everywhere (Hibernate checks the entities match the Flyway-managed schema at boot but never alters it). This is a deliberate choice: JPA is the runtime data-access layer, but schema evolution stays entirely in versioned SQL migration files, not annotation-driven auto-generation.

Schema lives in its own dedicated Postgres schema, `credit_consent` (not `public`) — set via `?currentSchema=credit_consent` in `BMS_DATABASE_URL` plus `spring.flyway.schemas`/`create-schemas: true`. `debezium.json`'s `table.include.list` (`credit_consent.tb_outbox_events`) must stay in sync if this schema name ever changes.

**⚠️ Every persistence change needs a new Flyway migration — never edit an applied one.** This covers both schema changes (new/altered columns, tables, indexes) and **data changes required by a new business rule** — e.g., adding a new product means a new `V{n}__seed_tb_terms_<product>.sql` inserting its `tb_terms` rows (see `V2__seed_tb_terms_consignado_dataprev.sql` for the pattern), not a manual `INSERT` run by hand against dev/hml/prod. Flyway checksums applied migrations; editing one that already ran in any environment breaks validation there. Add a new `V{n+1}__` file instead. The same rule applies to `ms-vivopay-credit-consent-worker-v1` (`src/main/resources/db/migration/`) for its own tables (`tb_processed_events`).

### Exception hierarchy

`BusinessException` is the base domain exception. Subclasses map to HTTP status via `ApiExceptionHandler` (RestControllerAdvice). All error responses follow `ProblemDetailResponse` format (errorCode, message, details, timestamp, traceId, errors list).

| Exception | HTTP Status |
|-----------|-------------|
| `MissingAuditDataException` | 400 |
| `InvalidTermException` | 422 |
| `MandatoryTermNotAcceptedException` | 422 |
| Unhandled `Exception` | 500 |

### Mappers

Domain VOs ↔ JPA entities are converted via custom repository classes + MapStruct mappers (`@Mapping`-annotated interfaces, `componentModel = "spring"`) — never manual `.builder()...build()` construction in service/repository code. Lombok + MapStruct annotation processors are both configured — keep the `lombok-mapstruct-binding` dependency when adding new mappers.

Key mappers:
- `CreditCoreMapper` (`core/application/mapper/creditcore/`) — ConsentRequestDTO ↔ VO, TermsCatalogVO → PendingTermVO, composes CustomerConsentVO
- `CustomerConsentRepositoryMapper` (`infrastructure/persistence/postgresql/mapper/`) — CustomerConsentVO ↔ CustomerConsentJpaEntity
- `TermsCatalogRepositoryMapper` (`infrastructure/persistence/postgresql/mapper/`) — `List<TermsCatalogJpaEntity>` → `List<TermsCatalogVO>`
- `OutBoxEventQueueMapper` (`core/application/mapper/outboxeventqueue/`) — `(consentId, payloadJson)` → `OutBoxEventQueueVO`, used by `CreditCoreServiceImpl` so the service never touches a JPA entity type directly
- `OutboxEventQueueRepositoryMapper` (`infrastructure/persistence/postgresql/mapper/`) — `OutBoxEventQueueVO` → `OutboxEventQueueJpaEntity`, used only inside `OutboxEventQueueCustomRepository`

The outbox mapping is deliberately split in two (VO-facing vs. entity-facing) so the service layer stays independent of the JPA entity types, matching the boundary the other two repositories already keep.

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

## Key Dependencies

- **Spring Boot** (lib-starter-parent:4.1.2), **Spring MVC** (Tomcat) + **Spring Data JPA**, **Flyway** (schema), Spring Data Redis (blocking, Lettuce client)
- **Azure Event Hubs** — outbound event publishing (`EventHubProducerClient`, blocking)
- **MapStruct 1.5.3** + **Lombok** — VO/entity mapping
- **java-jwt 4.4.0** + **jjwt 0.11.5** — JWT parsing in `JwtTokenUtils`
- **SpringDoc OpenAPI 3** — Swagger UI
- **Netflix Eureka Client** — service discovery
- **Observability** (`spring-boot-starter-opentelemetry`) — Micrometer Tracing (OTel bridge) + OTLP trace/metrics export, fully managed by the Boot BOM (no explicit version pin needed, unlike `resilience4j`-style dependencies — confirmed against the `spring-boot-dependencies:4.1.0` POM). See README's "Observabilidade — OpenTelemetry" section (worker repo) for the full end-to-end design; this repo's role is capturing the current span's W3C traceparent into `tb_outbox_events.trace_context` so Debezium can promote it to a Kafka header. See "Observability" section below for SQL/Redis span instrumentation.
- **Logs bridge** (`io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.21.0-alpha`) — pinned explicitly (not BOM-managed); its transitive `opentelemetry-api-incubator` must be excluded and repinned to `1.62.0-alpha` to match the SDK version Boot brings, or startup fails with `NoSuchMethodError`. Installed via `br.com.tlf.shared.observability.OpenTelemetryLoggingConfig` + an `OpenTelemetry` appender added to the existing `logback-spring.xml`.
- **Metrics scrape** (`io.micrometer:micrometer-registry-prometheus`) — no `<version>` (managed by the `micrometer-bom` `spring-boot-dependencies` imports), exposes `/actuator/prometheus` alongside the existing OTLP metrics push.
- **SQL spans** (`net.ttddyy.observation:datasource-micrometer-spring-boot` + `-opentelemetry`, `2.2.1`) — Boot 4.1 has no native JDBC/Observation instrumentation; this library wraps the `DataSource` via `datasource-proxy` internally, auto-configured (no manual bean). See "Observability" section below.
