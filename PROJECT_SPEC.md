# QUICKBASKET: MASTER PROJECT SPECIFICATION

> **Project Title**: QuickBasket (Quick-Commerce Price Comparison & Product Intelligence Platform)  
> **Target Role**: Java / Spring Boot Backend Engineer  
> **Architecture**: Modular Monolith  
> **Primary Stack**: Java 21, Spring Boot 3.2+, PostgreSQL 16, Redis 7, Resilience4j, Docker Compose  
> **Documentation Version**: 1.0.0  

---

## 1. Executive Summary

### What is QuickBasket?
QuickBasket is a modern Java/Spring Boot backend platform that aggregates, normalizes, and compares real-time product pricing, stock availability, discounts, and delivery ETAs across multiple quick-commerce platforms (e.g., Blinkit, Zepto, Swiggy Instamart, BigBasket) via third-party API integration.

### What Problem Does It Solve?
Quick-commerce shoppers in India face fragmented pricing and inventory. The exact same SKU (e.g., "Amul Taaza Milk 1L") varies in price, stock availability, discount percentage, and delivery time across different platforms. Users manually switch between 4 different apps to find the cheapest price or fastest delivery. QuickBasket unifies this data into a single search and analytical intelligence backend.

### Target Users
* **Smart Shoppers**: Budget-conscious consumers looking for the best price or fastest delivery.
* **Deal Hunters**: Power users tracking historical price drops and set target alerts.

### Why is it Technically Interesting?
* **Uncertain External Environments**: Integrates third-party APIs with unpredictable latency, schema shifts, rate limits, and failure modes.
* **Complex Backend Mechanics**: Implements provider abstractions, Redis cache-aside strategies, Resilience4j circuit breakers, asynchronous job queues, and timeseries data modeling in PostgreSQL.

### Career Value
Replaces generic, uninspiring projects (e.g., Employee Management Systems, standard CRUD apps) with a production-grade microservice-ready backend that answers real technical interview questions about caching, resiliency, third-party API integration, and performance optimization.

---

## 2. Goals & Non-Goals

### Primary Goals
1. Build a high-performance Java 21 / Spring Boot 3 backend that aggregates product data from external APIs.
2. Abstract third-party API logic behind clean provider interfaces (`ProductProvider`).
3. Implement Redis caching (Cache-Aside pattern) to keep search latencies under 10ms and protect API quotas.
4. Design a resilient system using Resilience4j (Circuit Breakers, Retries, Fallbacks).
5. Build a timeseries price history tracking engine with scheduled background workers (`@Scheduled`).
6. Containerize the full environment using Docker Compose and build CI pipelines with GitHub Actions and Testcontainers.

### Secondary Goals
1. Provide interactive OpenAPI / Swagger documentation (`/swagger-ui.html`).
2. Build a minimal, lightweight React / HTML dashboard to demonstrate the backend functionality.

### Explicit Non-Goals (DO NOT BUILD)
* ❌ **Automated Checkout / Ordering**: Do NOT attempt automated purchasing on external platforms (impossible without official APIs; violates TOS).
* ❌ **Payment Gateway Integration**: No Stripe, Razorpay, or payment workflows.
* ❌ **Real-Time GPS Tracking**: No live delivery driver map rendering.
* ❌ **Microservices from Day One**: Do NOT split into multiple microservices. Build a clean Modular Monolith.
* ❌ **Kafka / Kubernetes**: Do NOT introduce Kafka or K8s initially. Docker Compose and Spring `@Scheduled`/`@Async` are sufficient.

---

## 3. Feature Roadmap

```text
 ┌────────────────────────────────────────────────────────┐
 │ MVP (PHASE 1: Weeks 1 - 4)                             │
 │ • Unified Product Search across platforms               │
 │ • RestClient Integration + Provider Abstraction        │
 │ • Response Normalization (Canonical Domain Model)      │
 │ • PostgreSQL Snapshot Persistence                      │
 └───────────────────────────┬────────────────────────────┘
                             │
                             ▼
 ┌────────────────────────────────────────────────────────┐
 │ PHASE 2 (Weeks 5 - 8)                                  │
 │ • Redis Cache-Aside Layer (5-min Search TTL)          │
 │ • Spring Security 6 + JWT User Registration/Login      │
 │ • User Watchlists & Product Price History Charts       │
 └───────────────────────────┬────────────────────────────┘
                             │
                             ▼
 ┌────────────────────────────────────────────────────────┐
 │ PHASE 3 (Weeks 9 - 12)                                 │
 │ • Resilience4j Circuit Breakers, Retries & Fallbacks   │
 │ • Scheduled Background Price Tracking (@Scheduled)     │
 │ • Price Drop Email / Webhook Alerts (@Async)           │
 │ • Docker Compose + Testcontainers Integration Tests    │
 └────────────────────────────────────────────────────────┘
```

| Feature | Phase | Why It Exists | What You Learn | Difficulty | Required for Portfolio? |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Unified Search** | MVP | Core product requirement | REST API design, DTO mapping | Easy | **YES** |
| **Provider Abstraction** | MVP | Prevents locking to 1 API vendor | Java Interfaces, Strategy Pattern | Medium | **YES** |
| **PostgreSQL Offers DB**| MVP | Stores price/stock snapshots | JPA/Hibernate, Relational Modeling | Medium | **YES** |
| **Redis Search Cache** | Phase 2 | Protects API quota & sub-10ms response | Cache-Aside, Redis TTLs | Medium | **YES** |
| **JWT Authentication** | Phase 2 | Protects watchlist endpoints | Spring Security 6, JWT Filter | Medium | **YES** |
| **Timeseries History** | Phase 2 | Tracks price trends over 30 days | SQL Composite Indexing, Windowing | Medium | **YES** |
| **Scheduled Tracking** | Phase 3 | Automated price checks every 6h | `@Scheduled`, Background Workers | Medium | **YES** |
| **Circuit Breaker** | Phase 3 | Prevents cascade failures on API outage | Resilience4j, Fallbacks | Hard | **YES** |
| **Testcontainers** | Phase 3 | Real DB/Redis integration tests | JUnit 5, Docker Testcontainers | Hard | **YES** |

---

## 4. Architecture Strategy (Modular Monolith)

### System Architecture Diagram

```text
                                  ┌──────────────────────────┐
                                  │   React Web UI / Client  │
                                  └────────────┬─────────────┘
                                               │ HTTP / REST
                                               ▼
                                  ┌──────────────────────────┐
                                  │  Spring Boot REST API    │
                                  │  (Controllers + DTOs)    │
                                  └────────────┬─────────────┘
                                               │
               ┌───────────────────────────────┼───────────────────────────────┐
               │                               │                               │
               ▼                               ▼                               ▼
    ┌────────────────────┐          ┌────────────────────┐          ┌────────────────────┐
    │  Spring Data JPA   │          │    Redis Cache     │          │  Security & Auth   │
    │  (PostgreSQL DB)   │          │ (5-min Search TTL) │          │  (Spring Sec + JWT)│
    └──────────┬─────────┘          └────────────────────┘          └────────────────────┘
               │
               ▼
    ┌────────────────────────────────────────────────────────────────────────────────────┐
    │                         PROVIDER ABSTRACTION LAYER                                 │
    │                     `interface ProductProvider`                                    │
    └──────────────────────────────────────────┬─────────────────────────────────────────┘
                                               │
                                               ▼
    ┌────────────────────────────────────────────────────────────────────────────────────┐
    │               QuickCommerceApiProvider (RestClient + CircuitBreaker)               │
    └──────────────────────────────────────────┬─────────────────────────────────────────┘
                                               │
                                               ▼
    ┌────────────────────────────────────────────────────────────────────────────────────┐
    │                External Third-Party API (QuickCommerceAPI.com)                     │
    │                   [ Blinkit  |  Zepto  |  Instamart  |  BigBasket ]                │
    └────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Provider Abstraction Pattern

To ensure business logic is **never tightly coupled** to `quickcommerceapi.com`, we use the **Strategy Pattern**:

```java
public interface ProductProvider {
    List<NormalizedProductOffer> searchProducts(String query, String latitude, String longitude);
    boolean supports(String providerCode);
}
```

### Implementations:
1. **`QuickCommerceApiProvider`**: Calls `https://quickcommerceapi.com/` using Spring `RestClient`.
2. **`MockProductProvider`**: Returns realistic mock product payloads locally when developing offline or running unit tests without hitting third-party APIs.

---

## 6. External API Strategy & Risk Assessment

### Primary Data API: QuickCommerce API (`quickcommerceapi.com`)
* **Status**: **VERIFY** (Third-party aggregator service; NOT an official API exposed by Blinkit/Zepto).
* **Authentication**: Bearer Token / API Key header (`Authorization: Bearer <API_KEY>`).
* **Endpoints Used**:
  * `GET /api/v1/search?q={query}&lat={lat}&lng={lng}`
* **Rate Limits & Credits**: **VERIFY** (Assume 100 requests/min on standard tier).
* **Legal / TOS Risk**:
  * **Portfolio / GitHub / Learning**: **100% Safe and Appropriate**.
  * **Commercial Product Launch**: **High Risk** (Blinkit/Zepto change anti-scraping layouts or issue TOS blocks).
* **Mitigation**: Decouple behind `ProductProvider` interface.

---

## 7. Public APIs Repository Evaluation Matrix

We evaluated APIs from `https://github.com/public-apis/public-apis`:

| API Name | Category | Purpose in QuickBasket | Free Tier? | API Key? | Learning Value | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`IPinfo.io` / `ipapi.co`** | Geolocation | Auto-detect user latitude/longitude from IP address if GPS is disabled | Yes (50k req/mo) | Optional | Medium | **RECOMMENDED** |
| **`Open-Meteo API`** | Weather | Correlate delivery ETA delays with severe rain/storms | Yes (100% free) | No | Low | **OPTIONAL** |
| **`Resend` / `Mailgun`** | Email | Send HTML emails when watchlisted items drop below target price | Yes (3k emails/mo)| Yes | High | **RECOMMENDED** |
| **`Open Exchange Rates`**| Currency | Convert currency rates | Yes | Yes | Low | **NOT NEEDED** |
| **`OpenStreetMap / Nominatim`**| Maps | Address autocomplete to lat/lng coordinates | Yes (Free) | No | Medium | **OPTIONAL** |

---

## 8. Final Technology Stack Choices

* **Language**: **Java 21 (LTS)** (Records, Pattern Matching, Virtual Threads ready).
* **Framework**: **Spring Boot 3.2+** (`RestClient`, ProblemDetail error standard).
* **HTTP Client**: **Spring `RestClient`** (Modern fluent HTTP client).
* **Database**: **PostgreSQL 16** (Superior JSONB, indexing, and window functions).
* **Cache**: **Redis 7** (Spring Data Redis, Cache-Aside pattern).
* **Resiliency**: **Resilience4j** (Circuit Breaker, Retry, Fallback).
* **Testing**: **JUnit 5 + Mockito + Testcontainers** (Real PostgreSQL & Redis Docker test containers).
* **Documentation**: **OpenAPI 3 / Swagger** (`springdoc-openapi-starter-webmvc-ui`).
* **Containerization**: **Docker Compose** (App + Postgres + Redis).
* **CI/CD**: **GitHub Actions** (Automated build and test run on `git push`).
* **Frontend**: **Lightweight React (Vite + Tailwind CSS)**.

---

## 9. Error Handling Strategy (RFC 7807)

All API exceptions are intercepted by `@RestControllerAdvice` and returned using Spring 3.2's `ProblemDetail` (RFC 7807):

```json
{
  "type": "https://quickbasket.com/errors/external-api-failure",
  "title": "Third-Party Provider Unavailable",
  "status": 503,
  "detail": "QuickCommerce API experienced a timeout. Served fallback cached results.",
  "instance": "/api/v1/products/search",
  "timestamp": "2026-08-31T22:00:00Z"
}
```

---

## 10. Caching Strategy (Redis)

* **Pattern**: Cache-Aside.
* **Target Endpoint**: `GET /api/v1/products/search`
* **Cache Key Format**: `qb:search:{lat}:{lng}:{hash(query)}`
* **TTL**: **5 Minutes** (Quick-commerce prices change frequently; 5 mins balances fresh data with API quota protection).
* **Failure Resilience**: If Redis goes down, the application catches the `RedisConnectionException`, logs a warning, and falls back to database/API calls transparently.

---

## 11. Resilience Strategy (Resilience4j)

```text
                        [ RestClient Request ]
                                  │
                                  ▼
                   [ Resilience4j Circuit Breaker ]
                                  │
         ┌────────────────────────┴────────────────────────┐
         │ (State: CLOSED)                                 │ (State: OPEN / 50% Failures)
         ▼                                                 ▼
   [ Execute API Call ]                             [ Execute Fallback ]
         │                                                 │
   ┌─────┴─────────────┐                                   │
   ▼ (Success)         ▼ (Timeout / 5xx Error)             │
[ Return Data ]  [ Resilience4j Retry ]                    │
                       │                                   │
                 ┌─────┴─────────────┐                     │
                 ▼ (Retry Exhausted)  ▼                     │
                 └─────────┬─────────┘                     │
                           ▼                               ▼
                 [ Execute Fallback: Return Cached DB Snapshot / Empty List ]
```

* **Retry Policy**: Maximum 3 retries, 500ms initial interval, exponential backoff (multiplier = 2). Retries on `ResourceAccessException` (timeout) and HTTP 5xx errors.
* **HTTP 429 Rate Limit Policy**: Do **NOT** retry immediately on HTTP 429 (Too Many Requests). Immediately trigger fallback and log rate limit warning.

---

## 12. Authentication & Security

* **Security Framework**: Spring Security 6.
* **Authentication Method**: Stateless JWT (JSON Web Tokens).
* **Password Hashing**: BCrypt (`strength = 12`).
* **Secret Protection**: Third-party API keys (`QUICKCOMMERCE_API_KEY`) and JWT secrets (`JWT_SECRET`) are injected via environment variables and **NEVER** committed to Git.

---

## 13. Testing Strategy Pyramid

```text
                     / \
                    /   \     End-to-End Tests (Postman / Cypress)
                   /-----\
                  / Integration \   Testcontainers (Real Postgres & Redis)
                 /---------------\
                /   Unit Tests    \  JUnit 5 + Mockito (Services, Normalizer, Providers)
               ---------------------
```

---

## 14. Git & GitHub Repository Strategy

* **Repository Structure**: Monorepo layout containing `backend/`, `frontend/`, `docker-compose.yml`, and `README.md`.
* **Branching Strategy**: `main` (production-ready) $\rightarrow$ `feature/*` branches for development.
* **Commit Message Standard**: Conventional Commits (`feat: add product normalizer`, `fix: handle 429 rate limit`).
* **Secret Guard**: `.gitignore` configured to ignore `.env`, `target/`, `.idea/`, `*.jar`.

---

## 15. Package Structure

```text
com.quickbasket
├── config              # Spring beans, RestClient, Redis, Swagger, Async config
├── controller          # REST Controllers (@RestController)
├── dto                 # Request/Response Java Records
├── entity              # JPA Entities (@Entity)
├── repository          # Spring Data JPA Repositories
├── service             # Core Business Logic Services
│   ├── provider        # ProductProvider interface & implementations
│   └── normalizer      # Payload Transformation logic
├── exception           # Global Exception Handler & Custom Exceptions
├── security            # Spring Security filters, JWT utilities
├── scheduler           # Scheduled background cron jobs (@Scheduled)
└── util                # Constants and Helper utilities
```

---

## 16. Package Ratings & Difficulty Matrix

* **Career Value**: **9.5 / 10**
* **Learning Value**: **9.5 / 10**
* **Portfolio Value**: **9.0 / 10**
* **Technical Depth**: **9.0 / 10**
* **Difficulty Level**: **7.0 / 10 (Intermediate)**
* **Estimated Development Hours**: **80 – 100 Engineering Hours** (~8–12 calendar weeks)

---

## 17. Core Engineering Rules & Guardrails

1. **Start as a Modular Monolith**: Do NOT introduce microservices.
2. **Read-Only Third-Party Interaction**: Never attempt purchasing on external APIs.
3. **No Unearned Technologies**: Do NOT use Kafka, K8s, or complex tools without a demonstrated bottleneck.
4. **Decouple API Providers**: Always program to the `ProductProvider` interface.
5. **Protect API Keys**: Never commit secrets to version control.
