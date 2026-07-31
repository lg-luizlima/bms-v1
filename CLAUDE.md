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
- **shared** — Cross-cutting: utilities (`HmacUtils`, `JwtTokenUtils`, `MathUtils`), config beans (including `WebClientConfiguration`)

`spring-boot-starter-webflux`/`reactor-netty` are also on the classpath, but **not** used on the request path — Spring Boot picks the Servlet stack (Tomcat) because `spring-boot-starter-web` is present too (`DispatcherServlet` on the classpath wins regardless of WebFlux also being there). Webflux is kept solely so `WebClientConfiguration` (`shared/configuration/common/webclient/`) can build a `WebClient`/`reactor.netty.http.client.HttpClient` for outbound calls — don't be surprised to find it in the dependency tree of an otherwise-blocking service.

### Request flow

`CreditCoreController` → `CreditCorePortIn` → `CreditCoreServiceImpl` → out-port interfaces → infrastructure adapters

### REST Endpoints

Base path: `/credit-core/v1`

| Method | Path | Description | Auth Header | Response |
|--------|------|-------------|-------------|----------|
| GET | `/terms` | Get pending terms for a product | `authorization` | 200 `ActiveConsentResponseDTO` |
| POST | `/consents` | Create customer consent | `authorization` | 201 (no body) |

### Key port contracts

| Interface | Location | Adapter |
|-----------|----------|---------|
| `CreditCorePortIn` | `core/port/in/creditcore/` | `CreditCoreServiceImpl` |
| `CustomerConsentRepository` | `core/port/out/customerconsent/` | `CustomerConsentCustomRepository` (JPA) |
| `TermsCatalogRepository` | `core/port/out/termscatalog/` | `TermsCatalogCustomRepository` (JPA) |
| `OutboxEventQueueRepository` | `core/port/out/outbox/` | `OutboxEventQueueCustomRepository` (JPA) |
| `EventHubPort` | `core/port/out/eventhub/` | `EventHubAdapter` (`EventHubProducerClient`) |

**CustomerConsentRepository methods:**
- `CustomerConsentVO getActiveCustomerConsent(String cpfHash, String termCode)`
- `CustomerConsentVO saveConsent(CustomerConsentVO consent)`

**TermsCatalogRepository methods:**
- `List<TermsCatalogVO> findLatestActiveByProduct(String product)`

The consent + outbox write is wrapped in a single transaction via `TransactionTemplate` (`CreditCoreServiceImpl.saveConsentAtomically`), not `@Transactional` on a private method — self-invoked private methods are never intercepted by Spring's proxy-based AOP, so that annotation would silently be a no-op regardless of blocking vs. reactive.

### Domain model

The service manages versioned **terms catalogs** per product (e.g. `EP_INSS`) and records customer **consents** (accepted terms + audit signature). CPFs are hashed with `HmacUtils.generateHmacSha256` before use as `cpfHash` anywhere (repository lookups, Redis keys). Consent events are published to Azure Event Hub using the **outbox pattern** (`tb_outbox_events` table, consumed by Debezium CDC). Redis (`StringRedisTemplate`) caches processing status during consent creation with key `sync_status:{cpfHash}`.

**JPA entities** (`infrastructure/persistence/postgresql/entity/`):
- `CustomerConsentJpaEntity` — cpfHash (64-char), termCode, termId (FK, plain UUID column, no relation), optIn, acceptedAt, expiresAt, auditDetails (`@JdbcTypeCode(SqlTypes.JSON)` → Postgres `jsonb`, mapped as `String`)
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
| `BMS_DATABASE_URL` | PostgreSQL JDBC URL — used by both JPA (runtime) and Flyway (bootstrap migration) |
| `BMS_DATABASE_USERNAME` | DB username |
| `BMS_DATABASE_PASSWORD` | DB password |
| `EUREKA_REGISTER_URL` | Service discovery |
| `SPRING_PROFILES_ACTIVE` | Active profile |
| `EVENTHUB_CONNECTION_STRING` | Azure Event Hub connection |
| `EVENTHUB_NAME` | Azure Event Hub name |

## Key Dependencies

- **Spring Boot** (lib-starter-parent:4.1.2), **Spring MVC** (Tomcat) + **Spring Data JPA**, **Flyway** (schema), Spring Data Redis (blocking)
- **Spring WebFlux** — present but only backs `WebClientConfiguration`'s outbound `WebClient`, not the request path (see Architecture)
- **Azure Event Hubs** — outbound event publishing (`EventHubProducerClient`, blocking)
- **MapStruct 1.5.3** + **Lombok** — VO/entity mapping
- **java-jwt 4.4.0** + **jjwt 0.11.5** — JWT parsing in `JwtTokenUtils`
- **SpringDoc OpenAPI 3** — Swagger UI
- **Netflix Eureka Client** — service discovery
- **Observability** (`spring-boot-starter-opentelemetry`) — Micrometer Tracing (OTel bridge) + OTLP trace/metrics export, fully managed by the Boot BOM (no explicit version pin needed, unlike `resilience4j`-style dependencies — confirmed against the `spring-boot-dependencies:4.1.0` POM). See README's "Observabilidade — OpenTelemetry" section (worker repo) for the full end-to-end design; this repo's role is capturing the current span's W3C traceparent into `tb_outbox_events.trace_context` so Debezium can promote it to a Kafka header.
- **Logs bridge** (`io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.21.0-alpha`) — pinned explicitly (not BOM-managed); its transitive `opentelemetry-api-incubator` must be excluded and repinned to `1.62.0-alpha` to match the SDK version Boot brings, or startup fails with `NoSuchMethodError`. Installed via `br.com.tlf.shared.observability.OpenTelemetryLoggingConfig` + an `OpenTelemetry` appender added to the existing `logback-spring.xml`.
- **Metrics scrape** (`io.micrometer:micrometer-registry-prometheus`) — no `<version>` (managed by the `micrometer-bom` `spring-boot-dependencies` imports), exposes `/actuator/prometheus` alongside the existing OTLP metrics push.
