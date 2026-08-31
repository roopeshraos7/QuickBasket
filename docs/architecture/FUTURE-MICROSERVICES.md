# QUICKBASKET: FUTURE MICROSERVICES EVALUATION & MIGRATION STRATEGY

> **Document Status**: **FUTURE / NOT CURRENT IMPLEMENTATION**  
> **Current Architecture**: Modular Monolith (`com.quickbasket`)  
> **Purpose**: Document hypothetical extraction candidates, evaluation criteria, target microservice architecture, and migration roadmap if independent scaling or deployment requirements emerge in future phases.

---

## 1. Current State: Why Modular Monolith?

QuickBasket is intentionally implemented as a **Modular Monolith**.

### Key Benefits:
1. **Low Operational Overhead**: Single deployment artifact (`.jar`), single JVM process, zero network latency between internal service calls.
2. **ACID Transactions**: Local database transactions in PostgreSQL without distributed transaction patterns (SAGA / 2PC).
3. **Refactoring Flexibility**: Boundaries between components (`product`, `pricing`, `watchlist`) can be refined easily in Java before locking into network APIs.
4. **Focused Learning**: Primary focus remains on Java 21, Spring Boot 3.2, REST, provider abstraction, caching, and resiliency without premature Kubernetes/Kafka overhead.

---

## 2. Potential Microservice Extraction Candidates

If QuickBasket scales significantly, the following domains represent potential standalone microservices:

```text
 ┌──────────────────────┐      ┌──────────────────────┐      ┌──────────────────────┐
 │ Product / Search     │      │ Pricing & Offers     │      │ User & Watchlist     │
 │ Service              │      │ Service              │      │ Service              │
 │ • Unified Search     │      │ • Provider Polling   │      │ • JWT Auth           │
 │ • Redis Cache-Aside  │      │ • Price Snapshots    │      │ • User Watchlists    │
 └──────────────────────┘      └──────────────────────┘      └──────────────────────┘
            │                             │                             │
            └─────────────────────────────┼─────────────────────────────┘
                                          ▼
                               ┌──────────────────────┐
                               │ Price Tracking Worker│
                               │ • Cron Tracker Job   │
                               │ • Alert Notifications│
                               └──────────────────────┘
```

| Potential Service | Module Source | Responsibility | Justification |
| :--- | :--- | :--- | :--- |
| **Product Search Service** | `com.quickbasket.product` | Handles user search traffic (`GET /search`), Redis caching | High read traffic, needs horizontal REST scaling |
| **Pricing & Provider Service**| `com.quickbasket.service.provider` | Interacts with external APIs (`quickcommerceapi.com`) | Rate limits, circuit breaking, third-party network IO |
| **Price Tracking Worker** | `com.quickbasket.scheduler` | `@Scheduled` cron jobs polling price drops every 6h | CPU/IO intensive background processing |
| **User & Auth Service** | `com.quickbasket.security` / `watchlist` | Registration, JWT issuing, user watchlist management | Low-volume write traffic, security isolation |

---

## 3. Extraction Criteria

A module inside the modular monolith will ONLY be extracted into a standalone service if at least two of the following quantitative conditions are met:

1. **Independent Scaling Footprint**:
   * *Example*: Search query volume increases 50x while user registration volume remains flat.
2. **Resource Saturation / Heavy Background Processing**:
   * *Example*: Background price tracking cron jobs saturate JVM worker threads, increasing REST search p99 latency beyond 200ms.
3. **Deployment Velocity & Isolation**:
   * *Example*: Rapid changes to external provider API integrations require daily deployments without risking user auth stability.
4. **Resiliency Boundaries**:
   * *Example*: External API third-party outages must not cause memory starvation in core authentication services.

---

## 4. Target Potential Microservices Architecture

```text
                                  ┌──────────────────────────┐
                                  │   React Web UI / Client  │
                                  └────────────┬─────────────┘
                                               │
                                               ▼
                                  ┌──────────────────────────┐
                                  │   API Gateway (Spring)   │
                                  └────────────┬─────────────┘
                                               │
                ┌──────────────────────────────┼──────────────────────────────┐
                │                              │                              │
                ▼                              ▼                              ▼
     ┌────────────────────┐         ┌────────────────────┐         ┌────────────────────┐
     │ Product Search     │         │ Pricing & Provider │         │ User & Auth        │
     │ Service            │         │ Service            │         │ Service            │
     └──────────┬─────────┘         └──────────┬─────────┘         └──────────┬─────────┘
                │                              │                              │
          ┌─────┴─────┐                  ┌─────┴─────┐                  ┌─────┴─────┐
          ▼           ▼                  ▼           ▼                  ▼           ▼
       [Redis]   [Product DB]        [Redis]    [Pricing DB]         [User DB]  [Email SMTP]
```

> **IMPORTANT NOTE**: The diagram above is a **FUTURE PROJECTION FOR EDUCATIONAL PURPOSES**. It is **NOT** the current implementation architecture.

---

## 5. Theoretical Migration Strategy (Strangler Fig Pattern)

If microservice extraction becomes justified in a future phase, the migration will follow the **Strangler Fig Pattern**:

1. **Enforce Package Boundaries**: Ensure zero cross-package direct database access inside the modular monolith.
2. **Introduce Domain Events / Interfaces**: Replace direct Java method invocations between modules with explicit service interfaces or internal event publishing.
3. **Database Decoupling**: Separate database tables into domain schemas (`product_schema`, `user_schema`).
4. **Extract Target Module**: Move domain package into a separate Spring Boot application project.
5. **Route Traffic**: Update API Gateway / HTTP client to route specific endpoints to the new extracted service.
6. **Verify & Deprecate**: Measure latency, errors, and memory before decommissioning the legacy internal module.
