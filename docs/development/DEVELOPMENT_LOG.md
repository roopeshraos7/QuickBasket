# QUICKBASKET: ENGINEERING DEVELOPMENT LOG

> **Journal Purpose**: Chronological engineering log tracking phase milestones, architectural decisions, technologies introduced, trade-offs, and key learnings.

---

## 2026-08-31 — Project Setup & Governance Initialization

### Phase
Governance & Documentation Setup

### Branch
`main`

### What Changed
- Reorganized project documentation into structured folders under `docs/` (`architecture/`, `api/`, `database/`, `development/`, `learning/`).
- Moved existing spec files (`ARCHITECTURE.md`, `DECISIONS.md`, `API_SPEC.md`, `DATABASE.md`, `ROADMAP.md`, `LEARNING.md`) into `docs/` subdirectories preserving Git history.
- Created `docs/development/PROJECT_RULES.md` defining 30 core development, architectural, and git rules.
- Created `docs/architecture/FUTURE-MICROSERVICES.md` establishing modular monolith boundaries and criteria for future microservice extraction.
- Created `docs/development/DEVELOPMENT_LOG.md` as the living project journal.
- Updated `docs/architecture/ARCHITECTURE.md` to explicitly document modular monolith architecture and deferred microservices strategy.

### Why
To establish clean governance, branching discipline, living documentation standards, and portfolio-ready Git history prior to starting code implementation.

### Technologies
- Java 21 (Target)
- Spring Boot 3.2+ (Target)
- Git & Markdown

### Important Decisions
- **ADR-001 Enforcement**: Main branch is strictly reserved for stable documentation. Implementation occurs on non-deleted phase branches (`phase/<name>`).
- **Modular Monolith First**: Microservices are explicitly documented as potential future candidates, not initial architecture.

### What I Learned
- Git history preservation using `git mv`.
- Modular Monolith boundary design principles.
- Clean documentation taxonomy for production-grade projects.

### Next Step
Create initial phase branch `phase/week-1-project-setup` and initialize the Maven Spring Boot 3.2+ project.

---

## 2026-08-31 — AI, Open-Source & Cloud Strategy Update

### Phase
Architecture & Strategy Specification

### Branch
`main`

### What Changed
- Created `docs/architecture/AI-ARCHITECTURE.md` detailing optional open-source LLM integration (Ollama), NL query parsing, SKU matching, comparison explainer, and price trend summaries.
- Created `docs/learning/LEARNING.md` establishing the engineering knowledge matrix for interview preparation across Java 21, Spring Boot, DB, Caching, Security, DevOps, Cloud, and AI.
- Updated `docs/development/PROJECT_RULES.md` with 6 permanent AI & Cloud governance rules (AI Rule, Deterministic Core, AI Optionality, Open-Source, Cloud Learning, Cost Rule).
- Updated `docs/development/ROADMAP.md` adding Phase 11 (Cloud Deployment) and Phase 12 (AI Intelligence Layer).
- Updated `docs/architecture/DECISIONS.md` with ADR-009 (Optional Open-Source AI Strategy & Cloud Deployment).

### Why
To incorporate AI and cloud deployment into the project's long-term vision while maintaining Java 21 / Spring Boot backend engineering as the primary core objective.

### Technologies
- Ollama / Open-Weight LLMs (Qwen, Gemma, Llama) - *Future Phase 12*
- Free-tier Cloud Infrastructure - *Future Phase 11*

### Important Decisions
- **ADR-009**: AI is strictly an optional layer (`AI_ENABLED=false`). Core calculations remain 100% deterministic in Java.
- **Open-Source Priority**: Free, self-hosted LLM (Ollama) preferred for local development to avoid API costs.

### Next Step
Commence Week 1 implementation on phase/week-1-project-setup branch.

---

## 2026-08-31 — Week 1 / Phase 1: Core Spring Boot Setup, DTOs & Provider Abstraction

### Phase
Week 1 — Core Setup & Provider Abstraction

### Branch
`phase/week-1-project-setup`

### What Changed
- Initialized Maven Spring Boot 3.2.3 project with Java 21, Spring Web, Lombok, and Springdoc OpenAPI UI (`2.3.0`).
- Created Java 21 DTO records: `NormalizedProductOffer`, `BestOption`, `ProductSearchResponse`, `QuickCommerceRawOffer`, and `QuickCommerceRawResponse`.
- Implemented `ProductProvider` Strategy pattern interface (`searchProducts`, `supports`).
- Implemented `MockProductProvider` providing deterministic offline sample data for Blinkit, Zepto, and Instamart.
- Implemented `QuickCommerceApiProvider` using Spring 3.2 `RestClient` with configurable base URL and Bearer API token.
- Implemented `ProductComparisonService` calculating cheapest price and fastest ETA among in-stock offers.
- Implemented `ProductComparisonController` exposing `GET /api/v1/products/search` with input validation and OpenAPI annotations.
- Implemented `GlobalExceptionHandler` returning RFC 7807 `ProblemDetail` JSON responses for `400 Bad Request` and `503 Service Unavailable`.
- Created JUnit 5 & Mockito test suite (`MockProductProviderTest`, `ProductComparisonServiceTest`, `ProductComparisonControllerTest`).

### Why
To establish the core REST framework, strategy-based provider abstraction, and immutable API DTO models while ensuring 100% offline development capability.

### Technologies
- Java 21 (Records, Sealed patterns ready)
- Spring Boot 3.2.3 (`RestClient`, `ProblemDetail` RFC 7807)
- Lombok & Jackson
- Springdoc OpenAPI 2.3.0 (`/swagger-ui.html`)
- JUnit 5, Mockito & AssertJ

### Important Decisions
- **Strategy Pattern (`ProductProvider`)**: Insulates core search logic from external API changes (`ADR-005`).
- **Spring `RestClient`**: Adopted over `RestTemplate` / `WebClient` for synchronous, fluent HTTP integration (`ADR-003`).
- **RFC 7807 `ProblemDetail`**: Standardized REST error payload format.

### What I Learned
- Dependency Injection & Component Scanning in Spring Boot.
- Java 21 `record` immutability and accessor syntax.
- Strategy Pattern implementation in Spring using multi-bean injection (`List<ProductProvider>`).
- Spring 3.2 `RestClient` fluent builder API.
- `@WebMvcTest` controller testing and RFC 7807 assertions.

### Next Step
Wait for explicit instruction to proceed to Week 2 / Phase 2 (PostgreSQL Database Design & JPA Persistence).


