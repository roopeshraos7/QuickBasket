# QUICKBASKET: AI INTEGRATION ARCHITECTURE & OPEN-SOURCE STRATEGY

> **Document Status**: **FUTURE ARCHITECTURE / NOT CURRENT IMPLEMENTATION**  
> **Target Phase**: Phase 12 (AI Intelligence Layer)  
> **Core Principle**: QuickBasket is a **Java 21 / Spring Boot backend engineering project**. AI serves strictly as an **optional intelligence layer** on top of a deterministic backend, never replacing core business logic.

---

## 1. Core AI Philosophy & Scope Boundary

QuickBasket integrates AI to solve specific product intelligence problems—such as natural language query parsing, SKU cross-platform deduplication, and natural-language price trend explanations.

### Primary Rules:
1. **Java/Spring Backend First**: Core search, price comparison, caching, security, database persistence, and resilience remain 100% independent Java/Spring Boot code.
2. **Deterministic Source of Truth**: All numerical calculations (cheapest price, price diff, discount %, ETA ranking) are performed deterministically by Java code or PostgreSQL. AI never invents numerical facts.
3. **Strict Optionality**: The application supports `AI_ENABLED=false`. When AI is disabled or fails, all core search and price comparison features operate normally.

---

## 2. Capability 1: Natural-Language Product Search

Converts unstructured user queries into structured search criteria without exposing core business logic to non-deterministic LLM behavior.

### System Flow:
```text
User Natural Language ("Find low-fat milk under ₹70 within 15 mins")
                        │
                        ▼
      [ Spring Boot Controller / Service ]
                        │ (If AI_ENABLED=true)
                        ▼
           [ AiProvider.parseSearch() ]
                        │
                        ▼
          Structured JSON Search Parameters:
          {
            "category": "milk",
            "maxPrice": 70.00,
            "maxEtaMinutes": 15,
            "preferences": ["low-fat"]
          }
                        │
                        ▼
   [ Core Spring Boot ProductSearchService ] (Deterministic)
                        │
                        ▼
         [ ProductProvider (Strategy Pattern) ]
```

---

## 3. Capability 2: AI-Powered SKU & Product Matching

Different quick-commerce providers return slightly different names for identical items:

```text
Platform A: "Amul Taaza Toned Milk 1L"
Platform B: "Amul Taaza Milk - 1 Litre"
Platform C: "Amul Taaza Toned Milk 1000ml"
```

### Hybrid Matching Pipeline:
To map offers to a canonical product, QuickBasket uses a layered approach:

```text
Deterministic Rule Matching (Brand + Normalized Name + Quantity + Unit)
                        │
                        ▼ (If uncertain / low confidence)
  Vector Embedding Similarity / Open LLM Classifier (Cos Similarity > 0.88)
                        │
                        ▼
             Canonical SKU Assignment
```

*Deterministic matching is evaluated first. Embeddings/AI are invoked only when rule-based confidence is below threshold.*

---

## 4. Capability 3: AI-Assisted Comparison Explanations

The backend engine calculates cheapest, fastest, and discount metrics deterministically:

```text
Blinkit:   ₹54  | 14 mins
Zepto:     ₹56  | 10 mins
Instamart: ₹52  | 20 mins
```

### Fact-to-Natural-Language Translation:
The Java backend constructs a structured `ComparisonResult` record and passes it to the AI layer to generate a clear summary:

> **Generated Explanation**: *"Swiggy Instamart offers the lowest price at ₹52 (saving ₹4 over Zepto), but Blinkit delivers 6 minutes faster for an extra ₹2."*

*Rule*: AI explains backend-calculated facts. AI is **never** the source of truth for numerical calculations.

---

## 5. Capability 4: AI Price Trend Summaries

PostgreSQL computes timeseries window metrics (min price, max price, 30-day average, volatility). AI generates human-readable trend alerts.

```text
PostgreSQL Timeseries Data ──► Deterministic Math ──► Fact Payload ──► AI Explainer ──► User Summary
```

---

## 6. AI Provider Abstraction Pattern

Following the `ProductProvider` Strategy Pattern, all AI interactions are insulated behind an interface:

```java
public interface AiProvider {

    AiSearchCriteria parseSearchQuery(String naturalLanguageQuery);

    String explainComparison(ComparisonResult result);

    String summarizePriceTrend(PriceTrendMetrics metrics);

    boolean isAvailable();
}
```

### Target Implementations:
1. **`MockAiProvider`**: Offline deterministic responses for unit testing and local development without LLM runtime.
2. **`OllamaAiProvider`**: Local open-weight LLM client invoking self-hosted models (Qwen, Gemma, Llama) via Ollama REST API.
3. **`HostedAiProvider`** *(Optional)*: Integrates free-tier cloud AI APIs if hosted infrastructure is available.

---

## 7. Open-Source & Free-First AI Model Selection Strategy

QuickBasket adheres to a strict free & open-source cost model:

```text
Open-Source / Self-Hosted (Ollama)
                ↓
Free-Tier Cloud Provider
                ↓
Paid Service (Only if explicitly justified)
```

### Local Development Model Evaluation Criteria:
When Phase 12 begins, local open-weight models will be evaluated against:
* **Memory Footprint**: Fits within 4GB–8GB RAM allocation.
* **Structured Output Support**: JSON schema mode reliability.
* **Inference Speed**: Sub-1-second latency on standard CPU/GPU.
* **Candidate Models**: Qwen2.5-Coder / Qwen2.5-3B, Gemma 2 2B, Llama 3.2 1B/3B.

---

## 8. AI Fault Tolerance & Security Principles

1. **Circuit Breaking & Timeouts**: If `AiProvider` takes > 1500ms or throws an exception, the system falls back to standard search gracefully.
2. **Input Validation**: All structured JSON output from LLMs is validated by Jackson & Bean Validation before passing to core domain services.
3. **Secret Protection**: API keys are injected via environment variables (`AI_API_KEY`). Prompts never contain user PII or system credentials.
