# QUICKBASKET: 8–12 WEEK DEVELOPMENT & LEARNING ROADMAP

> **Schedule Commitment**: Weekdays: 1–1.5 hrs/day | Weekends: 2–3 hrs/day (~10–12 engineering hrs/week)

---

## 📅 WEEKS 1–2: Core Spring Boot Setup, RestClient & Provider Abstraction

### Topics to Learn
* Spring Boot 3.2+ setup, Spring `RestClient`, Java 21 `record`s.
* Strategy Pattern, Interface Abstraction (`ProductProvider`), DTO mapping.
* OpenAPI / Swagger configuration (`springdoc-openapi`).

### Coding Tasks
* Initialize Spring Boot project on [start.spring.io](https://start.spring.io) with dependencies (`Spring Web`, `Lombok`, `Springdoc`).
* Create Java DTO records: `NormalizedProductOffer`, `ProductSearchResponse`, `ProviderRawResponse`.
* Define `ProductProvider` interface.
* Implement `QuickCommerceApiProvider` using Spring `RestClient` to call `quickcommerceapi.com`.
* Implement `MockProductProvider` for offline development and local unit testing.
* Build `ProductComparisonController` with endpoint `GET /api/v1/products/search`.

### Expected Output
Swagger UI at `http://localhost:8080/swagger-ui.html` allows testing `GET /api/v1/products/search?q=milk` and returning normalized JSON offers from Mock/QuickCommerce providers.

### Git Milestone
`feat: implement core RestClient API provider abstraction and search endpoint`

### Interview Concepts Learned
* Third-party REST API integration, Strategy Pattern, `RestClient` vs `RestTemplate`, Java `record`s vs classes, API DTO decoupling.

---

## 📅 WEEKS 3–4: PostgreSQL Database Design & JPA Persistence

### Topics to Learn
* PostgreSQL 16 schema design, Spring Data JPA, Hibernate entity mappings (`@Entity`, `@OneToMany`, `@ManyToOne`), Database Indexing strategies.

### Coding Tasks
* Install PostgreSQL locally or via Docker (`docker run -p 5432:5432 postgres`).
* Create JPA Entities: `ProductEntity`, `PlatformEntity`, `PlatformOfferEntity`, `PriceHistoryEntity`.
* Create Spring Data JPA Repositories: `ProductRepository`, `PlatformOfferRepository`, `PriceHistoryRepository`.
* Implement `ProductCatalogService`: When search results arrive, insert/update current price snapshots in `platform_offers` and log historical price entries in `price_history`.
* Add SQL composite index on `price_history(product_id, platform_id, recorded_at DESC)`.

### Expected Output
Executing a search query automatically persists canonical product records, updates current price snapshots, and inserts timeseries records into PostgreSQL.

### Git Milestone
`feat: integrate PostgreSQL database and JPA timeseries price snapshot persistence`

### Interview Concepts Learned
* JPA entity mappings, N+1 query problem avoidance, transaction isolation (`@Transactional`), timeseries index design, Hibernate lifecycle.

---

## 📅 WEEKS 5–6: Redis Caching & Search Optimization

### Topics to Learn
* Redis data structures, Spring Data Redis, `@Cacheable` annotation, Cache-Aside Pattern, TTL management, Cache Stampede prevention.

### Coding Tasks
* Spin up Redis via Docker (`docker run -p 6379:6379 redis:7-alpine`).
* Add `spring-boot-starter-data-redis` dependency and configure `RedisCacheManager` with a 5-minute TTL.
* Annotate `ProductComparisonService.searchProducts()` with `@Cacheable(value="product_searches", key="#query + '_' + #lat + '_' + #lng")`.
* Implement Redis exception handling: If Redis connection drops, catch exception and execute database search transparently.

### Expected Output
First search takes ~500ms (API/DB call). Repeat searches within 5 minutes respond in < 5ms directly from Redis memory.

### Git Milestone
`feat: implement Redis cache-aside layer with 5-minute TTL for search queries`

### Interview Concepts Learned
* Cache-Aside pattern, Cache TTL trade-offs, Cache stampede mitigation, graceful degradation when cache fails.

---

## 📅 WEEKS 7–8: Spring Security 6 & JWT Authentication

### Topics to Learn
* Spring Security 6 Filter Chain, BCrypt password hashing, Stateless JWT (JSON Web Tokens) token generation & filter validation.

### Coding Tasks
* Add `spring-boot-starter-security` and `jjwt` dependencies.
* Create `UserEntity` and `UserRepository`.
* Build `AuthController` with endpoints: `POST /api/v1/auth/register` and `POST /api/v1/auth/login`.
* Create `JwtAuthenticationFilter` to validate Bearer tokens on protected endpoints.
* Implement `WatchlistController`: Secured endpoints `GET /api/v1/watchlists` and `POST /api/v1/watchlists`.

### Expected Output
Public endpoints (`/search`, `/auth/**`) remain open. Protected endpoints (`/watchlists`, `/price-alerts`) reject unauthenticated requests with `401 Unauthorized`.

### Git Milestone
`feat: implement Spring Security 6 JWT stateless authentication and watchlists`

### Interview Concepts Learned
* Stateless authentication filter chains, BCrypt hashing, JWT structure & security, Spring Security `SecurityContext`.

---

## 📅 WEEKS 9–10: Scheduled Price Tracking & Asynchronous Alert Engine

### Topics to Learn
* Spring `@Scheduled` cron jobs, `@Async` thread pools, TaskExecutors, Spring Mail (`JavaMailSender`).

### Coding Tasks
* Enable scheduling (`@EnableScheduling`) and async processing (`@EnableAsync`).
* Create `PriceAlertEntity` and `PriceAlertRepository` (`target_price`, `is_triggered`).
* Create `ScheduledPriceTracker`: Cron job running every 6 hours (`@Scheduled(cron = "0 0 */6 * * *")`) to poll watchlisted items and check target price thresholds.
* Create `AsyncNotificationService`: `@Async` method that sends HTML email alerts when price drops below target threshold.

### Expected Output
Cron job executes silently every 6 hours, updates prices in the database, and fires asynchronous email alerts when a user's price alert threshold is met.

### Git Milestone
`feat: implement scheduled price tracking cron job and async email notification engine`

### Interview Concepts Learned
* Cron expressions, `@Scheduled` task execution in Spring, thread pool configuration (`ThreadPoolTaskExecutor`), `@Async` non-blocking processing.

---

## 📅 WEEKS 11–12: Resilience4j, Testcontainers, Docker Compose & CI/CD

### Topics to Learn
* Resilience4j (CircuitBreaker, Retry, Fallback), Testcontainers integration testing, Docker Compose orchestration, GitHub Actions CI pipelines.

### Coding Tasks
* Add `io.github.resilience4j:resilience4j-spring-boot3` dependency.
* Configure `@CircuitBreaker` and `@Retry` on `QuickCommerceApiProvider` with fallback methods.
* Write integration tests using `Testcontainers` (launching real PostgreSQL and Redis containers during `mvn test`).
* Create `docker-compose.yml` orchestrating `quickbasket-app`, `postgres`, and `redis`.
* Create `.github/workflows/ci.yml` to automatically build, lint, and run tests on `git push`.

### Expected Output
`docker compose up` starts full stack cleanly. GitHub repository shows green passing build badge on commits.

### Git Milestone
`feat: configure Resilience4j fault tolerance, Testcontainers, Docker Compose, and CI pipeline`

### Interview Concepts Learned
* Circuit breaker states (CLOSED, OPEN, HALF_OPEN), integration testing vs unit testing, Testcontainers lifecycle, Docker container linking, CI/CD automation.
