# QUICKBASKET: ARCHITECTURAL DECISION RECORDS (ADR)

> **Purpose**: Document critical architecture decisions, context, and trade-offs.

---

## ADR-001: Modular Monolith Architecture over Microservices

* **Status**: **ACCEPTED**
* **Context**: Building a modern backend application for job portfolio and skill acquisition.
* **Decision**: Adopt a single-repo Modular Monolith structure instead of microservices.
* **Rationale**: Microservices introduce network latency, distributed transaction complexity (SAGA/2PC), and deployment overhead. A modular monolith provides clean domain separation without premature operational complexity.
* **Trade-Off**: Scaling requires deploying the entire monolith, but for target workloads this is vastly simpler and faster to build.

---

## ADR-002: PostgreSQL 16 over MySQL

* **Status**: **ACCEPTED**
* **Context**: Need a robust relational database engine for product catalogs, offers, and timeseries price history.
* **Decision**: Choose PostgreSQL 16 over MySQL 8.
* **Rationale**: PostgreSQL offers superior `JSONB` support, superior composite indexing for timeseries queries (`price_history`), advanced window functions, and is the preferred enterprise backend standard.
* **Trade-Off**: Slightly higher memory usage than minimal MySQL setups, easily handled via Docker Compose.

---

## ADR-003: Spring `RestClient` over `RestTemplate` / `WebClient`

* **Status**: **ACCEPTED**
* **Context**: Need a modern HTTP client to invoke third-party APIs from Spring Boot 3.2+.
* **Decision**: Use `RestClient` (introduced in Spring Boot 3.1 / Spring 6).
* **Rationale**: `RestTemplate` is in maintenance mode. `WebClient` requires pulling in `spring-boot-starter-webflux` (Reactive dependency). `RestClient` provides a synchronous, fluent API without reactive complexity.
* **Trade-Off**: Requires Spring Boot 3.1+.

---

## ADR-004: Redis Cache-Aside Pattern with 5-Minute TTL

* **Status**: **ACCEPTED**
* **Context**: External quick-commerce API endpoints have rate limits and latency overhead.
* **Decision**: Implement Redis Cache-Aside pattern on search endpoints (`GET /search`) with a 5-minute TTL.
* **Rationale**: 5 minutes balances data freshness (quick-commerce prices shift frequently) with 75%+ reduction in third-party API call volumes.
* **Trade-Off**: Stale data up to 5 minutes old may be served during price changes.

---

## ADR-005: Provider Abstraction Pattern (`ProductProvider`)

* **Status**: **ACCEPTED**
* **Context**: Third-party API (`quickcommerceapi.com`) is an unofficial aggregator subject to potential downtime or layout updates.
* **Decision**: Abstract external API calls behind a `ProductProvider` interface with `QuickCommerceApiProvider` and `MockProductProvider` implementations.
* **Rationale**: Decouples core business logic from external API providers. Allows swapping data providers or running offline unit tests without code changes.
* **Trade-Off**: Requires writing normalization wrapper logic (`ProductNormalizer`).

---

## ADR-006: Resilience4j Circuit Breaker & Fallback Strategy

* **Status**: **ACCEPTED**
* **Context**: Third-party API calls may experience network timeouts or 5xx server errors.
* **Decision**: Wrap external HTTP calls with Resilience4j `@CircuitBreaker` and `@Retry`.
* **Rationale**: If failures exceed 50%, circuit opens to prevent hanging application threads. Fallback method returns cached DB snapshots.
* **Trade-Off**: Requires configuring Resilience4j properties and writing fallback methods.

---

## ADR-007: Integration Strategy for Third-Party APIs

* **Status**: **ACCEPTED**
* **Context**: Evaluating APIs from `quickcommerceapi.com` and `public-apis`.
* **Decision**: Integrate `quickcommerceapi.com` for price comparisons, `IPinfo.io` for IP-geolocation, and `Resend`/SMTP for email alerts. Reject unnecessary weather/currency APIs.
* **Rationale**: Keeps external dependencies minimal, meaningful, and focused on core user value.
* **Trade-Off**: Must manage multiple API keys in environment variables.

---

## ADR-008: Docker Compose Local Orchestration

* **Status**: **ACCEPTED**
* **Context**: Managing multi-container dependencies (Spring Boot, PostgreSQL, Redis) during development.
* **Decision**: Provide a master `docker-compose.yml` file.
* **Rationale**: Allows spinning up the full application stack in 1 step (`docker compose up`) across Linux and Windows.
* **Trade-Off**: Requires Docker Desktop or Docker Engine installed on local machine.

---

## ADR-009: Optional Open-Source AI Strategy & Public Cloud Deployment

* **Status**: **ACCEPTED**
* **Context**: Adding AI intelligence capabilities (natural language query parsing, price trend summaries) and cloud deployment without overshadowing core Java 21 / Spring Boot backend learning goals.
* **Decision**: 
  1. Treat AI as an **optional, open-source intelligence layer** (`AiProvider` interface with `OllamaAiProvider` and `MockAiProvider`). Core backend business logic remains 100% deterministic and functional when `AI_ENABLED=false`.
  2. Implement cloud deployment using free-tier / open-source friendly infrastructure as a dedicated Phase 11 learning milestone prior to AI introduction.
* **Rationale**: Ensures the project remains focused on backend software engineering fundamentals while demonstrating production-style AI integration patterns without vendor lock-in or recurring API costs.
* **Trade-Off**: Local development requires running Ollama for local LLM inference when AI features are tested; cloud deployments execute with `AI_ENABLED=false` or lightweight hosted inference models.

