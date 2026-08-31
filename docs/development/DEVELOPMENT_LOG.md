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
Await user confirmation to create branch `phase/week-1-project-setup` and commence Maven Spring Boot project setup.

