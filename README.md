# QuickBasket 🛒⚡

> **Quick-Commerce Price Comparison & Product Intelligence Platform**  
> A modern Java 21 & Spring Boot 3.2+ Modular Monolith Backend.

---

## 📌 Project Overview
QuickBasket aggregates, normalizes, and compares real-time product pricing, stock availability, discounts, and delivery ETAs across multiple quick-commerce services (e.g. Blinkit, Zepto, Swiggy Instamart, BigBasket) via third-party API integration.

## 🛠️ Tech Stack
* **Language**: Java 21 (LTS)
* **Framework**: Spring Boot 3.2+ (`RestClient`, ProblemDetail errors)
* **Database**: PostgreSQL 16
* **Cache**: Redis 7 (Cache-Aside pattern, 5-min TTL)
* **Resiliency**: Resilience4j (Circuit Breaker, Retry, Fallback)
* **Authentication**: Spring Security 6 + Stateless JWT
* **Documentation**: OpenAPI 3 / Swagger (`/swagger-ui.html`)
* **Infrastructure**: Docker & Docker Compose
* **Testing**: JUnit 5, Mockito, Testcontainers

---

## 📚 Master Specifications & Planning Documents

All architectural and project specifications are documented in the repository:

* 📄 **[`PROJECT_SPEC.md`](PROJECT_SPEC.md)**: Master project specification & feature roadmap.
* 🏗️ **[`ARCHITECTURE.md`](ARCHITECTURE.md)**: System architecture, sequence diagrams & resiliency flow.
* 🔌 **[`API_SPEC.md`](API_SPEC.md)**: REST API specification & RFC 7807 error format.
* 🗄️ **[`DATABASE.md`](DATABASE.md)**: PostgreSQL ERD, DDL scripts & composite indexing strategy.
* 🗺️ **[`ROADMAP.md`](ROADMAP.md)**: Detailed 8–12 week development and learning roadmap.
* 🎓 **[`LEARNING.md`](LEARNING.md)**: Technology-by-technology interview preparation matrix.
* 📋 **[`DECISIONS.md`](DECISIONS.md)**: Architectural Decision Records (ADRs 001–008).

---

## 🚀 Quick Start (Development Setup)

### Prerequisites
* Java 21 JDK
* Maven 3.9+
* Docker Desktop / Docker Engine

### Local Infrastructure Setup
```bash
# Start PostgreSQL & Redis containers
docker compose up -d postgres redis
```

### Run Spring Boot Application
```bash
# Build and run application
mvn clean spring-boot:run
```

Access Swagger API Documentation at: `http://localhost:8080/swagger-ui.html`

---

## 📄 License
This project is open-source under the [MIT License](LICENSE).
