# QUICKBASKET: SYSTEM ARCHITECTURE SPECIFICATION

> **Architecture Style**: Modular Monolith  
> **Target Environment**: Single-node containerized deployment (Docker Compose)  
> **Key Quality Attributes**: Resiliency, Low Latency (< 10ms cached), Provider Decoupling  

---

## 1. High-Level Modular Monolith Overview

QuickBasket will initially use a modular monolith architecture. As the project evolves, we will evaluate whether individual modules should eventually be extracted into independently deployable services. QuickBasket starts as a modular monolith. Future extraction into microservices may be evaluated later based on actual technical requirements and measured system characteristics. Microservice adoption is not currently committed.

Rather than prematurely splitting into distributed microservices (which introduces network latency, distributed transactions, and deployment overhead), the application is organized into decoupled domain modules inside a single Spring Boot executable.

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

## 2. End-to-End Data Flow Sequence

### Scenario: User Product Search (`GET /api/v1/products/search?q=milk&lat=12.97&lng=77.59`)

```text
User / Client         Controller         ProductService       Redis Cache       ProductProvider     QuickCommerce API     PostgreSQL
     │                    │                    │                   │                   │                    │                   │
     │── GET /search ────►│                    │                   │                   │                    │                   │
     │                    │── searchProducts ─►│                   │                   │                    │                   │
     │                    │                    │── Check Cache ───►│                   │                    │                   │
     │                    │                    │◄─ CACHE MISS ─────│                   │                    │                   │
     │                    │                    │                                       │                    │                   │
     │                    │                    │── search(query, lat, lng) ───────────►│                    │                   │
     │                    │                    │                                       │── GET /search ────►│                   │
     │                    │                    │                                       │◄─ HTTP 200 JSON ───│                   │
     │                    │                    │◄─ List<NormalizedProductOffer> ───────│                    │                   │
     │                    │                    │                                                                                │
     │                    │                    │── Put Cache (TTL 5m) ────────────►│                                        │
     │                    │                    │                                                                                │
     │                    │                    │── Save Price History Async ───────────────────────────────────────────────────►│
     │                    │                    │                                                                                │
     │◄── HTTP 200 JSON ──│◄─ ProductSearchDTO │                                                                                │
```

---

## 3. Provider Abstraction Pattern

To insulate QuickBasket from changes, anti-scraping updates, or downtime in `quickcommerceapi.com`, all external product lookups pass through a provider contract:

```java
package com.quickbasket.service.provider;

import com.quickbasket.dto.NormalizedProductOffer;
import java.util.List;

public interface ProductProvider {
    List<NormalizedProductOffer> searchProducts(String query, String latitude, String longitude);
    boolean supports(String providerCode);
}
```

### Registered Providers:
1. **`QuickCommerceApiProvider`**: Calls `https://quickcommerceapi.com/` endpoints using Spring 3.2 `RestClient`.
2. **`MockProductProvider`**: Returns local deterministic test offers for offline development, local UI building, and JUnit tests.

---

## 4. Resilience & Fault Tolerance Architecture

QuickBasket integrates **Resilience4j** annotations around `QuickCommerceApiProvider`:

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

### Resilience Parameters:
* **CircuitBreaker Sliding Window**: 10 calls. If 50% fail or time out (> 2000ms), circuit opens for 30 seconds.
* **Retry**: Max 3 retries, 500ms initial delay, exponential backoff (multiplier = 2).
* **HTTP 429 Rate Limit Handling**: Do NOT retry HTTP 429. Fallback immediately to cache/database snapshot.

---

## 5. Redis Caching Architecture

* **Pattern**: Cache-Aside (Lazy Loading).
* **Key Format**: `qb:search:{lat}:{lng}:{SHA256(query)}`
* **TTL**: 300 seconds (5 minutes).
* **Graceful Degradation**: If Redis throws a connection exception, `RedisCacheManager` catches the exception, logs an alert, and executes the search against DB/API transparently.

---

## 6. Scheduled Background Processing Architecture

```text
 ┌────────────────────────────────────────────────────────┐
 │ `@Scheduled` Cron Worker (Runs every 6 hours)           │
 └───────────────────────────┬────────────────────────────┘
                             │
                             ▼
 ┌────────────────────────────────────────────────────────┐
 │ Fetch Active User Watchlists & Target Price Alerts     │
 └───────────────────────────┬────────────────────────────┘
                             │
                             ▼
 ┌────────────────────────────────────────────────────────┐
 │ Execute Product Search via `ProductProvider`            │
 └───────────────────────────┬────────────────────────────┘
                             │
                             ▼
 ┌────────────────────────────────────────────────────────┐
 │ Update PostgreSQL `price_history` & `platform_offers`  │
 └───────────────────────────┬────────────────────────────┘
                             │
                             ▼
 ┌────────────────────────────────────────────────────────┐
 │ Check `price <= target_price` Threshold                │
 └───────────────────────────┬────────────────────────────┘
                             │ (If True)
                             ▼
 ┌────────────────────────────────────────────────────────┐
 │ `@Async` Notification Worker: Send Email Alert via SMTP│
 └────────────────────────────────────────────────────────┘
```
