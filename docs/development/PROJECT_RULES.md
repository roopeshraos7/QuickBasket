# QUICKBASKET: PERMANENT DEVELOPMENT RULES & GOVERNANCE

> **Repository Rulebook**: Permanent standards, engineering principles, and git workflow rules for QuickBasket.  
> **Target Stack**: Java 21, Spring Boot 3.2+, PostgreSQL 16, Redis 7, Resilience4j, Docker Compose  
> **Architecture**: Modular Monolith  

---

## 1. Project Philosophy & Priority Hierarchy

QuickBasket is a production-grade Java 21 / Spring Boot backend engineering project designed for learning, clean software craft, and technical depth.

All engineering decisions follow this strict priority hierarchy:

```text
Correctness
    ↓
Security
    ↓
Maintainability
    ↓
Testability
    ↓
Observability
    ↓
Performance
    ↓
Scalability
```

* **No Premature Optimization**: Do not optimize without measurement.
* **No Unearned Technology**: Understand every dependency introduced.
* **Portfolio & Interview Quality**: Maintain enterprise-grade code cleanliness, Git hygiene, and detailed documentation.

---

## 2. Architecture Standard — Modular Monolith First

QuickBasket starts and remains a **Modular Monolith**.

```text
                    QuickBasket
                        │
              Spring Boot Application
                        │
        ┌───────────────┼────────────────┐
        │               │                │
     Product         Pricing          User/Auth
      Module          Module            Module
        │               │                │
        └───────────────┼────────────────┘
                        │
                 PostgreSQL / Redis
```

* High cohesive domain packages (`com.quickbasket.product`, `com.quickbasket.pricing`, `com.quickbasket.auth`) with loose coupling.
* Microservices are **NOT** used during initial phases.
* Provider abstractions insulate core domain logic from external third-party API changes (`ProductProvider` Strategy Pattern).

---

## 3. Microservice Extraction Evaluation Criteria

QuickBasket starts as a modular monolith. Future extraction into microservices may be evaluated later based on actual technical requirements and measured system characteristics. Microservice adoption is not currently committed.

Microservices will ONLY be considered if one or more of the following conditions are met:
1. **Independent Scaling**: A specific module (e.g. search polling) requires 10x compute/memory scaling compared to the rest of the app.
2. **Independent Deployment**: Deployment frequency or risk profile differs drastically between modules.
3. **Heavy Background Processing**: Background worker jobs saturate CPU/network looper threads, degrading REST responsiveness.
4. **Targeted Resilience & Availability**: Failure in one module must not impact availability of independent domain functions.

*Document candidates and hypothetical extraction paths in `docs/architecture/FUTURE-MICROSERVICES.md`.*

---

## 4. Git & Branching Governance

### Main Branch Protection (`main`)
* `main` is reserved exclusively for **Stable Documentation, Architecture, ADRs, and Project Governance**.
* **NO IMPLEMENTATION CODE IS ALLOWED DIRECTLY ON `main`**.

### Phase Branch Strategy
All development work happens on dedicated phase branches created from `main` or the latest stable checkpoint:

```text
main
 │
 ├── phase/week-1-project-setup
 ├── phase/week-2-provider-integration
 ├── phase/week-3-database
 ├── phase/week-4-price-comparison
 ├── phase/week-5-redis
 ├── phase/week-6-security
 ├── phase/week-7-price-tracking
 ├── phase/week-8-resilience
 ├── phase/week-9-testing
 └── phase/week-10-docker-cicd
```

### Mandatory Branch Retention
* **NEVER DELETE PHASE BRANCHES**.
* Completed phase branches remain in the repository permanently to preserve engineering evolution, learning progression, and interview discussion history.

---

## 5. Commit Standard & Quality

Every commit must follow conventional commit naming with a **mandatory body explanation**:

### Subject Standard:
```text
<type>(<scope>): <short description>
```
*Types*: `feat`, `fix`, `docs`, `test`, `refactor`, `chore`, `perf`.

### Commit Body Standard:
Explain:
1. **What** changed.
2. **Why** it was implemented.
3. **Design decisions** & trade-offs.
4. **Verification / tests** performed.

*Never use vague commit messages like `update`, `fix`, `changes`, `stuff`, or `done`.*

---

## 6. Living Documentation & Engineering Log

Documentation is an active part of implementation.
* Every meaningful architectural or technical change requires updating the relevant documentation under `docs/`.
* `docs/development/DEVELOPMENT_LOG.md` must be updated after every completed development milestone.
* Architectural decisions must be documented in `docs/architecture/DECISIONS.md` as formal ADRs.

---

## 7. Technology & Dependency Discipline

* Do not add dependencies or technologies simply because they are popular.
* Document every technology choice with: Problem, Why Needed, Alternatives Considered, Complexity Introduced.
* No premature caching (Redis), messaging (Kafka), or container orchestration (Kubernetes) until explicitly called for in the phase roadmap.

---

## 8. Data & Third-Party Integrity

* **No Hardcoded Secrets**: Secrets and API keys must be loaded from environment variables/configuration profiles.
* **No Unverifiable Assumptions**: Mark unverified external API assumptions as `VERIFY`.
* **No Fake Metrics**: Never claim unmeasured performance metrics (e.g., "75% faster") without empirical benchmarks.

---

## 9. Code Quality & Testing Rules

* **Explicit DTOs**: Never expose JPA `@Entity` classes directly through REST APIs. Use Java 21 `record` DTOs.
* **Constructor Injection**: Use standard constructor injection for Spring components (`@RequiredArgsConstructor` or explicit constructors).
* **Progressive Testing**: Write unit tests alongside business logic and integration tests with Testcontainers in testing phases.
* **Verification Before Commit**: Always run `git status`, `git diff`, and `mvn test` before committing changes.

---

## 10. Phase Completion Definition

A phase is complete ONLY when:
1. Implementation is functional and adheres to specs.
2. Unit / integration tests pass (`mvn test`).
3. Documentation (`docs/`) is updated.
4. `DEVELOPMENT_LOG.md` entry is added.
5. Working tree is clean (`git status`).
6. Conventional commit with detailed body is created.
7. Phase branch is pushed to remote repository.
8. Next phase branch is branched cleanly for subsequent work.

---

## 11. AI & Cloud Governance Rules

* **AI Rule**: AI must solve a demonstrated product or engineering problem. AI must not be introduced merely because it is currently popular.
* **Deterministic Core Rule**: Core business logic, calculations, pricing comparisons, availability decisions, and numerical analytics must remain deterministic and independently testable. AI must never invent underlying numerical data.
* **AI Optionality Rule**: QuickBasket must remain fully functional when AI services are unavailable (`AI_ENABLED=false`).
* **Open-Source Rule**: Prefer open-source technologies and free/self-hosted solutions (e.g., Ollama) whenever they provide a reasonable technical solution.
* **Cloud Learning Rule**: Cloud deployment is part of the project's learning objectives, not merely a hosting requirement.
* **Cost Rule**: Avoid introducing paid services when an adequate free or open-source alternative exists.

