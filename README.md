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
* 🏗️ **[`ARCHITECTURE.md`](docs/architecture/ARCHITECTURE.md)**: System architecture, sequence diagrams & resiliency flow.
* 🔮 **[`FUTURE-MICROSERVICES.md`](docs/architecture/FUTURE-MICROSERVICES.md)**: Modular Monolith vs Microservices evaluation.
* 📋 **[`DECISIONS.md`](docs/architecture/DECISIONS.md)**: Architectural Decision Records (ADRs 001–008).
* 🔌 **[`API_SPEC.md`](docs/api/API_SPEC.md)**: REST API specification & RFC 7807 error format.
* 🗄️ **[`DATABASE.md`](docs/database/DATABASE.md)**: PostgreSQL ERD, DDL scripts & composite indexing strategy.
* 📏 **[`PROJECT_RULES.md`](docs/development/PROJECT_RULES.md)**: Permanent development rules & project governance.
* 🗺️ **[`ROADMAP.md`](docs/development/ROADMAP.md)**: Detailed 8–12 week development and learning roadmap.
* 📝 **[`DEVELOPMENT_LOG.md`](docs/development/DEVELOPMENT_LOG.md)**: Chronological engineering journal.
* 🎓 **[`LEARNING.md`](docs/learning/LEARNING.md)**: Technology-by-technology interview preparation matrix.

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
