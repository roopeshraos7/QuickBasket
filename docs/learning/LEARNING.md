# QUICKBASKET: ENGINEERING KNOWLEDGE & INTERVIEW PREPARATION MATRIX

> **Document Purpose**: Continuously updated repository journal tracking core Java, Spring Boot, system design, database, cloud, and AI engineering concepts learned while implementing QuickBasket.  
> **Rule**: This document is updated dynamically as each technical feature is implemented.

---

## 1. Java 21 & Modern Language Features

### Core Concepts & Strategy
* **Java 21 LTS**: Virtual Threads (Project Loom), Sealed Classes, Pattern Matching for switch, Record Patterns.
* **Java `record`s**: Immutable data carriers used for API DTOs (`NormalizedProductOffer`, `ProductSearchResponse`). Replaces verbose Lombok `@Value` / POJOs for DTOs.
* **Strategy Pattern**: Used for `ProductProvider` and `AiProvider` abstractions to decouple core business logic from third-party vendor APIs.

### Technology Matrix:
| Topic | What It Is | Why We Use It | Where Used in QuickBasket | Key Takeaway / Interview Question |
| :--- | :--- | :--- | :--- | :--- |
| **Java Records** | Immutable data transparent carrier in Java 17+ | Reduces DTO boilerplate (constructors, getters, equals/hashCode) | `dto/*.java` | *Q: Why use records for DTOs?* A: Thread-safe, shallow immutable, concise, auto-generated component accessors. |
| **Strategy Pattern** | Behavioral pattern family of interchangeable algorithms | Decouples product search logic from vendor API integrations | `service/provider/ProductProvider.java` | *Q: How does Spring support Strategy pattern?* A: Spring auto-injects all matching beans into `List<ProductProvider>`. |

---

## 2. Spring Boot 3.2+ & Web Framework

### Core Concepts & Strategy
* **Dependency Injection (DI)**: Spring IoC container injects dependencies via constructors (`ProductComparisonService`), encouraging testability with Mockito.
* **Spring `RestClient`**: Modern synchronous HTTP client introduced in Spring 6 / Spring Boot 3.1. Replaces legacy `RestTemplate` without WebFlux overhead.
* **OpenAPI 3 / Swagger**: Auto-generates interactive API UI documentation at `/swagger-ui.html`.
* **`ProblemDetail` (RFC 7807)**: Standardized HTTP error responses for `@RestControllerAdvice`.

### Technology Matrix:
| Topic | What It Is | Why We Use It | Where Used in QuickBasket | Key Takeaway / Interview Question |
| :--- | :--- | :--- | :--- | :--- |
| **Dependency Injection** | IoC principle where Spring manages bean lifecycles | Promotes loose coupling, easier mocking during unit tests | `ProductComparisonService.java` | *Q: Why prefer constructor injection over `@Autowired` field injection?* A: Immutability (final fields), no reflection overhead, easier unit testing. |
| **Spring `RestClient`** | Fluent synchronous HTTP client | Clean API, built-in Jackson mapping, non-reactive | `QuickCommerceApiProvider.java` | *Q: Why choose RestClient over WebClient or RestTemplate?* A: RestTemplate is in maintenance mode; WebClient requires WebFlux reactive overhead. |
| **ProblemDetail** | RFC 7807 spec error response format | Consistent API error structure across all endpoints | `exception/GlobalExceptionHandler.java` | *Q: How do you standardize REST API error handling in Spring Boot 3?* A: Use `@RestControllerAdvice` returning `ProblemDetail`. |
| **`@WebMvcTest`** | Spring Boot test slice annotation for MVC controllers | Unit tests HTTP mapping/validation without full server startup | `ProductComparisonControllerTest.java` | *Q: What is the difference between `@SpringBootTest` and `@WebMvcTest`?* A: `@WebMvcTest` loads only web layer beans (Controllers, Converters), mock-injecting services for faster tests. |

---

## 3. Databases & Persistence (PostgreSQL 16 & JPA)

### Core Concepts & Strategy
* **Spring Data JPA**: Abstraction layer built on Hibernate ORM providing automatic repository implementation (`JpaRepository`).
* **JPA Entity Mapping**: `@Entity`, `@Table`, `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`, `@Column`, `@ManyToOne(fetch = FetchType.LAZY)`.
* **Composite Indexing**: Optimized SQL queries for timeseries price history (`product_id, platform_id, recorded_at DESC`).
* **Spring `@Transactional`**: Transaction demarcation ensuring atomic snapshot upserts and timeseries history logging.

### Technology Matrix:
| Topic | What It Is | Why We Use It | Where Used in QuickBasket | Key Takeaway / Interview Question |
| :--- | :--- | :--- | :--- | :--- |
| **Spring Data JPA** | Data access abstraction on top of JPA/Hibernate | Eliminates JDBC boilerplate, provides CRUD/Paging methods | `repository/*.java` | *Q: How do custom queries work in Spring Data JPA?* A: Derived query methods parsing method names (e.g. `findByCode`). |
| **FetchType.LAZY** | Hibernate entity loading strategy | Prevents N+1 query overhead by fetching related entities on demand | `PlatformOfferEntity.java` | *Q: Why prefer LAZY over EAGER loading for `@ManyToOne`?* A: EAGER executes immediate SQL joins/queries, degrading performance. |
| **Composite Indexes** | B-Tree index across multiple columns | Accelerates 30-day historical price lookup queries | `price_history` table | *Q: Why ordering matters in composite indexes?* A: Leftmost column rule dictates query filtering matching index structure. |
| **`@DataJpaTest`** | Spring Boot test slice for JPA repositories | Configures in-memory database & Hibernate for isolated DB testing | `ProductRepositoryTest.java` | *Q: How does `@DataJpaTest` isolate tests?* A: Rolls back transactions automatically after each test method. |

---

## 4. Caching & Performance (Redis 7)

### Core Concepts & Strategy
* **Cache-Aside Pattern**: Application reads from Redis first; on cache miss, fetches from database/API and populates Redis for 5 minutes.
* **Spring Cache Abstraction**: Declarative caching using `@EnableCaching` and `@Cacheable` SpEL expressions (`key = "'qb:search:' + ..."`).
* **Redis Serializers**: Using `GenericJackson2JsonRedisSerializer` for human-readable JSON payload storage instead of default Java JDK binary serialization.
* **Graceful Failure Degradation**: Implementing custom `CacheErrorHandler` to catch Redis connection outages (`RedisConnectionFailureException`) and proceed seamlessly to DB/API search without throwing HTTP 500 errors.

### Technology Matrix:
| Topic | What It Is | Why We Use It | Where Used in QuickBasket | Key Takeaway / Interview Question |
| :--- | :--- | :--- | :--- | :--- |
| **Cache-Aside Pattern** | Strategy where application explicitly manages cache loading | Protects external API quota and achieves sub-10ms response time | `ProductComparisonService.java` | *Q: What is Cache-Aside vs Read-Through?* A: In Cache-Aside, the application code handles cache lookup, fallback, and population. |
| **Spring `@Cacheable`** | SpEL-driven declarative caching annotation | Eliminates manual Redis template boilerplate | `searchProducts()` method | *Q: How does SpEL construct cache keys dynamically?* A: Evaluates method parameter expressions (e.g. `#query.toLowerCase().trim()`). |
| **JSON Serialization** | Storing cached values as structured JSON strings | Interoperable across services, human-readable in Redis CLI | `RedisCacheConfig.java` | *Q: Why avoid default Java JDK serialization in Redis?* A: Binary blobs are unreadable, tightly coupled to Java class signatures, and brittle. |
| **Cache Error Handling** | Intercepting Redis runtime connectivity failures | Ensures system resilience during Redis outages | `RedisCacheConfig.java` | *Q: How do you prevent Redis outages from taking down your backend?* A: Implement `CacheErrorHandler` to log warnings and bypass cache transparently. |

---

## 5. Security & Authentication (Spring Security 6 & JWT)
*(To be populated during Weeks 7–8 implementation)*

---

## 6. Background Jobs & Resilience (Resilience4j & Cron)
*(To be populated during Weeks 9–10 & 11–12 implementation)*

---

## 7. Containerization & DevOps (Docker, Testcontainers, CI/CD)
*(To be populated during Weeks 11–12 implementation)*

---

## 8. Cloud Infrastructure & Deployment
*(To be populated during Weeks 13–14 implementation)*

---

## 9. AI Integration & Open-Source LLMs (Ollama)
*(To be populated during Weeks 15–16 implementation)*

---

## 10. System Design & Architectural Decisions
* **ADR-001: Modular Monolith**: Avoids microservices overhead during initial phases.
* **ADR-005: Provider Abstraction**: Shields core backend from third-party schema drift.
* **ADR-009: Optional Open-Source AI Strategy**: Keeps backend 100% functional when AI is disabled.
