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

---

## ADR-010: Deterministic Product Matching & External SKU Identification

* **Status**: **ACCEPTED**
* **Context**: External providers return vendor-specific SKUs and item names. We need a clean strategy to uniquely identify platform offers and group them into canonical products in PostgreSQL without premature AI or vector database complexity.
* **Decision**: 
  1. `PlatformOfferEntity` uniquely identifies vendor items using the composite key `(platform_id, external_item_id)`.
  2. Canonical `ProductEntity` matching uses deterministic normalized attributes where available (`brand + normalized product name + quantity + unit`). Search terms are NOT used directly as canonical product names.
  3. The matching component (`ProductMatcher`) is modularized so it can evolve from simple deterministic rules (Phase 2) to enhanced SKU matching, and eventually optional AI-assisted matching (Phase 12) without breaking the core domain or database schema.
* **Rationale**: Maintains 100% deterministic correctness and isolation of database entity records while leaving a clean path for architectural evolution.
* **Trade-Off**: Basic rule-based matching may create separate `ProductEntity` entries for slight spelling variations until enhanced rule matching is added.

---

## ADR-011: Unified Provider Strategy for Quick-Commerce and E-Commerce (Amazon & Flipkart)

* **Status**: **PROPOSED / APPROVED IN DESIGN**
* **Context**: QuickBasket is expanding from quick-commerce (Blinkit, Zepto, Instamart, BigBasket) to traditional e-commerce (Amazon, Flipkart). We need a clean provider architecture that handles different delivery semantics (minutes vs. days) and seller dynamics without breaking the existing core domain.
* **Decision**:
  1. **Retain `ProductProvider` as Core Strategy**: Keep `ProductProvider` as the unified Strategy interface. Avoid creating separate top-level `CommerceProvider` interfaces.
  2. **Classify Platform Types**: Add `platform_type` (`QUICK_COMMERCE` vs `ECOMMERCE`) to `PlatformEntity` and `platforms` DB table.
  3. **Domain Offer Extension**: Extend `NormalizedProductOffer` DTO with optional `sellerName`, `deliveryEtaText` (e.g. "Tomorrow by 10 PM", "2-3 Days"), and `shippingFee`.
  4. **Parallel Provider Execution & Fault Tolerance**: Refactor `ProductComparisonService` to execute active providers concurrently using `CompletableFuture` with individual exception handling so single-provider outages or API rate limit failures do not block the search response.
  5. **API Credential Resilience**: Amazon PA-API 5.0 requires 3 qualifying sales within 30 days. Configure `AmazonProvider` and `FlipkartProvider` with feature flags (`quickbasket.providers.amazon.enabled: false`) and mock fallbacks (`MockECommerceProvider`) for local dev when official API eligibility is absent.
* **Rationale**: Preserves existing clean API contracts while supporting multi-seller e-commerce marketplaces and fast quick-commerce platforms in a single unified architecture.
* **Trade-Off**: E-commerce titles are significantly longer and require title normalization heuristics during SKU matching.



