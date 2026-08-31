# QUICKBASKET: TECHNOLOGY LEARNING & INTERVIEW PREPARATION PLAN

> **Focus**: Grounded backend engineering mastery without fake resume claims.

---

## 1. Technology Learning Matrix

| Technology | Core Concepts to Master | Where It Is Used in QuickBasket | Technical Interview Question Enabled |
| :--- | :--- | :--- | :--- |
| **Java 21** | Records, Pattern Matching, Sealed Classes, Virtual Threads. | DTOs, domain models, concurrent search tasks. | *"What are the benefits of Java Records over traditional POJOs?"* |
| **Spring Boot 3.2+** | Spring Web, `RestClient`, `@RestControllerAdvice`, ProblemDetail (RFC 7807). | REST API layer, third-party HTTP client, global exception handling. | *"Why choose Spring RestClient over legacy RestTemplate or WebClient?"* |
| **PostgreSQL 16** | Relational DDL, Composite Indexing, Timeseries logging, `JSONB`. | Persistent product catalog, price snapshots, timeseries history. | *"How did you structure composite indexes for timeseries trend queries?"* |
| **Spring Data JPA** | Entities, Repositories, N+1 query problem, `@Transactional`. | ORM layer interfacing with PostgreSQL tables. | *"How did you prevent the N+1 SELECT problem in Hibernate?"* |
| **Redis 7** | Cache-Aside pattern, TTLs, Key naming strategies, Cache stampedes. | 5-minute search query payload caching. | *"How do you prevent cache stampedes when a popular search key expires?"* |
| **Spring Security 6** | Stateless Filter Chain, BCrypt, JWT parsing. | User authentication and protected watchlist endpoints. | *"How does stateless JWT authentication work in Spring Security 6?"* |
| **Resilience4j** | Circuit Breaker, Retry, Fallback handling, HTTP 429 logic. | Fault tolerance around external QuickCommerce API client. | *"What happens to user requests when the third-party API experiences 50% failures?"* |
| **Testcontainers** | Integration testing, Container lifecycles, JUnit 5 extensions. | Real database/cache tests during Maven build phase. | *"How do you test JPA repositories without using fake H2 databases?"* |
| **Docker Compose** | Services, Volumes, Networks, Environment variables. | Local orchestration of App + Postgres + Redis. | *"How do you pass environment secrets into containerized Spring Boot apps?"* |

---

## 2. Categorized Technical Interview Questions Catalog

### A. Java & Core Concepts
* **Q**: *Why use Java 21 `record`s for API DTOs?*
  * **Concept**: Immutable data carriers, automatic `equals()`, `hashCode()`, `toString()`, zero boilerplate.

### B. Spring Boot & REST Architecture
* **Q**: *How did you handle third-party API failures in Spring Boot?*
  * **Concept**: Resilience4j Circuit Breakers, Retry policies with exponential backoff, `@CircuitBreaker` fallback methods returning cached/snapshot responses.

### C. Database & JPA Performance
* **Q**: *What is the N+1 problem in JPA and how did you resolve it?*
  * **Concept**: Executing N additional SQL queries when fetching parent entities. Resolved using `JOIN FETCH` queries in `@Query` definitions.

### D. Caching & Redis
* **Q**: *What is the Cache-Aside pattern and why did you pick a 5-minute TTL?*
  * **Concept**: Application checks Redis first; on miss, queries DB/API, populates Redis, and returns. 5-min TTL balances fresh prices with API quota protection.

### E. Security & JWT
* **Q**: *How are API keys for third-party services protected?*
  * **Concept**: Injected via environment variables (`QUICKCOMMERCE_API_KEY`), never exposed to frontend or Git.

### F. Resiliency & System Design
* **Q**: *How would you scale QuickBasket to handle 50,000 requests per minute?*
  * **Concept**: Scale Redis cluster, configure PostgreSQL read-replicas, use Apache Kafka for background price alert workers.
