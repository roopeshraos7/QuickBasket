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
*(To be populated during Weeks 3–4 implementation)*

---

## 4. Caching & Performance (Redis 7)
*(To be populated during Weeks 5–6 implementation)*

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
