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

## ADR-011: Multi-Provider Strategy for Quick-Commerce and E-Commerce (Amazon Creators API & Flipkart)

* **Status**: **APPROVED IN DESIGN**
* **Context**: QuickBasket is expanding from quick-commerce (Blinkit, Zepto, Instamart, BigBasket) to traditional e-commerce (Amazon, Flipkart). We need a resilient provider architecture that handles different delivery semantics (minutes vs. days), multi-seller dynamics, API eligibility constraints, and high-concurrency external I/O without degrading core system performance.
* **Decision**:
  1. **Unified Provider Strategy**: Retain `ProductProvider` as the core Strategy interface. Introduce `PlatformType` enum (`QUICK_COMMERCE`, `ECOMMERCE`) and add `platform_type` column to `platforms` table.
  2. **Amazon Creators API Migration**: Target the official **Amazon Creators API** (OAuth 2.0 Client Credentials) replacing deprecated PA-API 5.0. Because Creators API requires 10 qualifying sales/30 days, implement `AmazonCreatorsApiProvider` behind a feature flag (`quickbasket.providers.amazon.enabled: false`) with a `MockECommerceProvider` fallback for local dev.
  3. **Structured Delivery Model**: Encapsulate offer delivery in a `DeliveryEstimate` DTO (`type`: INSTANT/EXPRESS/STANDARD, `etaMinutes`, `deliveryText`, `shippingFee`) rather than assuming quick-commerce minutes only.
  4. **Multi-Pack SKU Matching (ADR-010 Expansion)**: Enhance title parsing to extract brand, single-unit volume (`1000ml`), and pack multiplier (`Pack of 2` -> `pack2`), creating deterministic canonical key `brand_name_totalvolume_packCount` to prevent matching 1L single items with 2L multi-packs.
  5. **Java 21 Virtual Thread Concurrency**: Enable Spring Boot 3.2 Virtual Threads (`spring.threads.virtual.enabled: true`). Execute provider HTTP requests concurrently using `CompletableFuture.supplyAsync(..., applicationTaskExecutor)` with 1.5s per-provider timeouts and fault isolation (`.exceptionally(...)`).
  6. **Per-Provider Redis Slice Caching**: Cache individual provider response slices in Redis under `qb:provider:{code}:{lat}:{lng}:{query}` with 5-minute TTL to allow instant partial cache hits and dynamic category aggregation.
* **Rationale**: Preserves clean API contracts, guarantees sub-second response times across 6+ providers, and insulates the backend from external API eligibility blocks.
* **Trade-Off**: Per-provider Redis caching requires pipeline/MGET assembly in `ProductComparisonService`.

---

## ADR-012: Integration Provider Identity vs. Consumer Platform Identity

* **Status**: **ACCEPTED**
* **Context**: QuickBasket integrates both multi-platform aggregator APIs (e.g., QuickCommerceAPI.com returning Blinkit, Zepto, Instamart) and direct platform integrations (e.g. Amazon, Flipkart). We need a clear architectural separation between upstream integration sources and downstream consumer marketplaces to ensure accurate timeout configuration, fault isolation reporting (`failedProviders`), and future per-provider Redis slice keying (`qb:provider:<provider_code>:<query>`).
* **Decision**:
  1. **Provider Identity (`providerCode`)**: Unique code representing the specific upstream integration source bean executed by `ProductComparisonService` (e.g. `MOCK`, `QUICKCOMMERCE_API`, `FLIPKART`, `AMAZON`). Configured under `quickbasket.providers.<provider-code>`, loaded into `ProductProvider.getProviderCode()`, and returned in `failedProviders` when an integration fails.
  2. **Platform Identity (`platformCode`)**: Unique code on `NormalizedProductOffer` representing the actual retail marketplace (e.g. `BLINKIT`, `ZEPTO`, `INSTAMART`, `BIGBASKET`, `FLIPKART`, `AMAZON`).
  3. **Aggregator Strategy**: Aggregator integration beans (such as `QuickCommerceApiProvider`) remain single `ProductProvider` beans while producing offers for multiple consumer platforms (`BLINKIT`, `ZEPTO`, `INSTAMART`).
* **Rationale**: Insulates API consumer fault reporting and Redis slice caching key definitions from vendor aggregation topologies while preserving individual consumer platform identification on offer records.
* **Trade-Off**: If an upstream aggregator API fails, `failedProviders` reports the integration code (`QUICKCOMMERCE_API`) rather than individual downstream platforms unless the aggregator API exposes platform-level status metadata.

---

## ADR-013: Per-Provider Redis Slice Caching Strategy

* **Status**: **ACCEPTED**
* **Context**: Whole-search caching (`qb:search:<query>`) prevented partial cache hits when provider availability or response times varied, duplicated offer payloads in Redis, and caused dual-invalidation stampedes across full search responses. We need granular per-provider caching that isolates provider integration responses while dynamically calculating the global `BestOption`.
* **Decision**:
  1. **Remove Whole-Search Caching**: Removed `@Cacheable` from `ProductComparisonService.searchProducts(...)`. Replaced with per-provider slice caching.
  2. **Service-Layer Slice Caching (`ProviderSliceCacheService`)**: Cache entries stored in Redis under the `provider_slices` namespace using key format `qb:provider:<PROVIDER_CODE>:<NORMALIZED_QUERY>:<LATITUDE>:<LONGITUDE>` with a 5-minute TTL (`300s`).
  3. **Cached Payload**: `List<NormalizedProductOffer>` for a single provider. `ProductSearchResponse`, `BestOption`, and `failedProviders` are **never** stored in Redis.
  4. **Dynamic Aggregation & BestOption**: `BestOption` is recalculated in Java memory after joining all returned slices (hits + fresh misses).
  5. **Database Persistence Bypass**: Fresh provider responses (Cache MISS) trigger PostgreSQL persistence (`catalogService.saveOffers()`); cached slices (Cache HIT) bypass DB writes to avoid redundant SQL inserts.
  6. **Resilience & Fallback**: Provider timeouts and exceptions are isolated, recorded in `failedProviders`, and returned as partial results without caching failure entries. Redis connection failures degrade gracefully via custom `CacheErrorHandler`.
* **Rationale**: Maximizes cache hit rates across heterogeneous provider APIs, reduces Redis memory overhead, guarantees accurate sub-10ms response times, and eliminates cache stampedes.
* **Trade-Off**: `BestOption` calculation and slice merging execute in memory on every search request (sub-1ms CPU cost).







