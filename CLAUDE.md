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
```

Server runs on port **8082** by default. Swagger UI available at `/swagger-ui.html`.

## Testing

```bash
./mvnw test                                          # all tests
./mvnw test -Dtest=ClassName                         # single class
./mvnw test -Dtest=ClassName#methodName              # single method
```

Tests use Mockito (`@ExtendWith(MockitoExtension.class)`) — no Spring context loaded, no H2. Test fixtures live in `src/test/java/br/com/tlf/dummies/`:
- `ConsentRequestDummies` — request/response DTOs and VOs; constants `CPF_PLAIN`, `BEARER_TOKEN`
- `CreditTermDummies` — `TermsCatalogVO` fixtures (revokedTerm, softTerm)
- `CustomerConsentDummies` — `CustomerConsentVO` fixtures

## Architecture

Hexagonal (Ports & Adapters), Java 21, virtual threads enabled. Four top-level packages under `br.com.tlf`:

- **api** — REST layer: controllers, request/response DTOs, exception handlers
- **core** — Domain: port interfaces (`port/in`, `port/out`), application services, domain VOs, exceptions
- **infrastructure** — Outbound adapters: PostgreSQL JPA, Azure Event Hub, outbox pattern
- **shared** — Cross-cutting: utilities (`HmacUtils`, `JwtTokenUtils`, `MathUtils`), config beans

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
| `EventHubPort` | `core/port/out/eventhub/` | `EventHubAdapter` |

**CustomerConsentRepository methods:**
- `Boolean getActiveConsent(String cpfHash, String termCode)`
- `Optional<UUID> getActiveConsentTermId(String cpfHash, String termCode)`
- `void saveConsent(CustomerConsentVO consent)`

**TermsCatalogRepository methods:**
- `List<TermsCatalogVO> findLatestActiveByProduct(String product)`

### Domain model

The service manages versioned **terms catalogs** per product (e.g. `EP_INSS`) and records customer **consents** (accepted terms + audit signature). CPFs are stored as HMAC-SHA256 hashes. Consent events are published to Azure Event Hub using the **outbox pattern** (`outbox_event_queue` table). Redis caches processing status during consent creation with key `sync_status:{cpf}`.

**JPA entities:**
- `CustomerConsentEntity` — cpfHash (64-char), termCode, termId (FK), optIn, acceptedAt, expiresAt, auditDetails (JSON)
- `TermsCatalogEntity` — product, termCode, version, isMandatory, validityDays, revokePreviousVersions, contentType, contentSummary, contentText, contentUrl, startAt, endAt
- `OutboxEventQueueEntity` — aggregateType, aggregateId, topicName, payload (JSON), createdAt

### Exception hierarchy

`BusinessException` is the base domain exception. Subclasses map to HTTP status via `ApiExceptionHandler` (RestControllerAdvice). All error responses follow `ProblemDetailResponse` format (errorCode, message, details, timestamp, traceId, errors list).

| Exception | HTTP Status |
|-----------|-------------|
| `MissingAuditDataException` | 400 |
| `InvalidTermException` | 422 |
| `MandatoryTermNotAcceptedException` | 422 |
| Unhandled `Exception` | 500 |

### Mappers

Domain VOs ↔ JPA entities are converted via custom repository classes + MapStruct mappers in `core/application/mapper/`. Lombok + MapStruct annotation processors are both configured — keep the `lombok-mapstruct-binding` dependency when adding new mappers.

Key mappers:
- `CreditCoreMapper` — ConsentRequestDTO ↔ VO, TermsCatalogVO → PendingTermVO, composes CustomerConsentVO
- `CustomerConsentRepositoryMapper` — CustomerConsentVO ↔ CustomerConsentEntity
- `TermsCatalogRepositoryMapper` — List<TermsCatalogEntity> → List<TermsCatalogVO>
- `OutBoxEventQueueMapper` — (consentId, payloadJson) → OutboxEventQueueEntity

## Profiles & Environment

Default profile is `local`. Production profiles (`dev`, `hml`, `prod`) require:

| Variable | Purpose |
|----------|---------|
| `DATABASE_URL` | PostgreSQL JDBC URL |
| `DATABASE_USER` | DB username |
| `DATABASE_PASSWORD` | DB password |
| `EUREKA_REGISTER_URL` | Service discovery |
| `SPRING_PROFILES_ACTIVE` | Active profile |
| `EVENT_HUB_CONNECTION_STRING` | Azure Event Hub connection |
| `EVENT_HUB_NAME` | Azure Event Hub name |

## Key Dependencies

- **Spring Boot** (lib-starter-parent:2.1.3), Spring Data JPA, Spring Data Redis, Spring WebFlux
- **Azure Event Hubs** — outbound event publishing
- **MapStruct 1.5.3** + **Lombok** — VO/entity mapping
- **java-jwt 4.4.0** + **jjwt 0.11.5** — JWT parsing in `JwtTokenUtils`
- **SpringDoc OpenAPI 3** — Swagger UI
- **Netflix Eureka Client** — service discovery
- Internal: `lib-spring-exception-handling`, `lib-fintech-log`, `lib-java-cloud-abstract-layer`, `lib-java-spring-security-starter`, `lib-crypto-management`
