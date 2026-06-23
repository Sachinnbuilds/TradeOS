# TraderOS — Product Requirements & High-Level Design Document

**Version:** 1.0  
**Author:** Draft for University Project  
**Date:** June 2026  
**Status:** Planning

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Problem Statement](#2-problem-statement)
3. [Goals & Non-Goals](#3-goals--non-goals)
4. [Target User](#4-target-user)
5. [Feature Requirements](#5-feature-requirements)
6. [Design Principles](#6-design-principles)
7. [Technology Stack](#7-technology-stack)
8. [High-Level Architecture](#8-high-level-architecture)
9. [Service Breakdown](#9-service-breakdown)
10. [Data Model](#10-data-model)
11. [AI Integration Design](#11-ai-integration-design)
12. [Event Design](#12-event-design)
13. [API Design](#13-api-design)
14. [Infrastructure & Deployment](#14-infrastructure--deployment)
15. [Build Phases](#15-build-phases)
16. [Open Questions & Risks](#16-open-questions--risks)

---

## 1. Executive Summary

TraderOS is an AI-native personal trading intelligence system built for active retail traders. It is not a dashboard — it is a system that autonomously analyzes your trades, learns your behavioral patterns, and actively intervenes to help you improve.

The platform ingests raw trade exports from brokers, processes them through an event-driven pipeline, applies AI to detect behavioral patterns and generate contextual debriefs, and exposes a natural language interface to query your own trading history.

The project is scoped for personal use by a single active trader, with a university project timeline of 8 weeks. It is designed to demonstrate proficiency in Java Spring Boot microservices, event-driven distributed systems, and practical AI integration using RAG and LLMs.

---

## 2. Problem Statement

Active retail traders lack a unified system to manage their trading process. Critical problems include:

- **No accountability loop** — Rules are created but never enforced. Mistakes like revenge trading, oversizing after a loss, and ignoring stop-losses are repeated without awareness.
- **No structured learning** — Traders remember large wins and losses but cannot answer: which strategy works? Which time of day am I profitable? What mistakes cost me the most?
- **No behavioral intelligence** — No tool detects patterns like "you oversize after three consecutive losses" or "your win rate drops below 30% after 2:30 PM."
- **High friction journaling** — Manual journaling requires discipline most traders don't maintain. The act of journaling must be made frictionless.

---

## 3. Goals & Non-Goals

### Goals

- Ingest trade data from broker CSV exports (Zerodha format initially)
- Calculate accurate trade-level and aggregate P&L
- Detect behavioral patterns autonomously using scheduled analysis jobs
- Generate AI-powered contextual trade debriefs without requiring manual journaling
- Allow natural language queries over personal trade history using RAG
- Enforce user-defined trading rules expressed in natural language
- Demonstrate event-driven architecture using Kafka/Redpanda
- Demonstrate AI integration with LLMs and vector embeddings

### Non-Goals

- Real-time broker API integration (out of scope)
- Multi-user support (personal use only)
- Tax computation (too complex for scope)
- Mobile application
- Multi-broker normalization beyond Zerodha CSV
- Portfolio tracking across crypto or international markets

---

## 4. Target User

**Primary:** The developer themselves — an active retail trader who trades Indian equities, futures, and options on Zerodha.

**Usage pattern:**
- Uploads weekly or daily trade CSVs
- Reviews AI-generated debrief at end of trading day
- Queries trade history conversationally
- Checks rule compliance weekly

---

## 5. Feature Requirements

### F1 — Trade Ingestion Pipeline

| ID | Requirement |
|----|-------------|
| F1.1 | Accept Zerodha trade book CSV upload via REST endpoint |
| F1.2 | Parse, validate, and normalize trades to internal schema |
| F1.3 | Publish each validated trade as an event to the message bus |
| F1.4 | Idempotent ingestion — duplicate trades must be detected and rejected |
| F1.5 | Support manual single trade entry via API |

### F2 — P&L Computation Engine

| ID | Requirement |
|----|-------------|
| F2.1 | Calculate realized P&L per trade using FIFO cost basis |
| F2.2 | Track open positions and unrealized P&L |
| F2.3 | Compute brokerage, STT, and charges from Zerodha fee schedule |
| F2.4 | Aggregate P&L by day, week, month, strategy tag |
| F2.5 | Compute win rate, average win, average loss, expectancy, and max drawdown |

### F3 — Natural Language Rule Engine

| ID | Requirement |
|----|-------------|
| F3.1 | Accept trading rules expressed in plain English via API |
| F3.2 | Use LLM to parse natural language rule into structured rule schema |
| F3.3 | Evaluate rules against every new trade event from the pipeline |
| F3.4 | Flag rule violations and store them with the offending trade |
| F3.5 | Expose rule compliance rate over time as a queryable metric |

**Example rules the engine must parse:**
- "Never risk more than 1% of capital on a single trade"
- "Stop trading after 3 consecutive losses in a day"
- "Do not trade Nifty options after 2:30 PM"
- "Maximum 5 trades per day"

### F4 — AI Trade Debrief Service

| ID | Requirement |
|----|-------------|
| F4.1 | Generate a contextual debrief every evening for that day's trades |
| F4.2 | Debrief must reference the trader's historical patterns, not just today's trades |
| F4.3 | Include specific questions for trades that are statistical outliers |
| F4.4 | Accept trader's natural language response and extract structured journal tags |
| F4.5 | Store extracted tags (strategy, emotion, mistake type, market condition) against each trade |
| F4.6 | Debrief is accessible via API and optionally via email |

**Example debrief output:**
> "You took 4 trades today. Your P&L is ₹-3,200. I noticed your 3rd trade was 2.8x your average position size. Historically, oversized trades follow a loss for you — this matches that pattern. What was your reasoning? Also, all 3 losses today occurred after 2 PM. Your post-2PM win rate is 19%. Do you want to add a rule to stop after 2 PM?"

### F5 — Behavioral Pattern Detection

| ID | Requirement |
|----|-------------|
| F5.1 | Run a nightly Spring Batch job across full trade history |
| F5.2 | Detect revenge trading signature: loss followed by immediate re-entry with ≥1.5x size |
| F5.3 | Detect overtrading correlation: days with high trade count vs. win rate |
| F5.4 | Detect time-of-day performance degradation |
| F5.5 | Detect strategy drift: discrepancy between stated strategy tags and actual trade behavior |
| F5.6 | Use LLM to narrate detected patterns in plain English |
| F5.7 | Produce a weekly pattern report stored and queryable |

### F6 — RAG Query Interface

| ID | Requirement |
|----|-------------|
| F6.1 | Embed all trades, journal entries, and pattern reports into pgvector |
| F6.2 | Accept natural language queries over trade history via REST API |
| F6.3 | Retrieve relevant trade context from vector store |
| F6.4 | Use LLM to synthesize answer with retrieved context and return with numbers |
| F6.5 | Support a chat-style multi-turn query session |

**Example queries the system must answer:**
- "What is my win rate on Bank Nifty options on expiry days?"
- "How much money have I lost to trades I tagged as revenge trades?"
- "Which strategy has the best risk-reward ratio over the last 3 months?"
- "On which days of the week do I perform worst?"

---

## 6. Design Principles

### P1 — Event-Driven Over Request-Driven

Every trade ingested publishes an event. Downstream processing (P&L calculation, rule evaluation, pattern detection triggers) is decoupled from the ingestion act. Services react to events, not API calls. This makes the system extensible — adding a new consumer does not change existing code.

### P2 — AI as Infrastructure, Not Feature

The LLM is not a chatbot bolted on top. It is wired into the core processing pipeline: rule parsing, debrief generation, pattern narration, and query answering all use the LLM as a computation engine. The AI removes friction from workflows that would otherwise require manual discipline.

### P3 — Single Responsibility Per Service

Each microservice owns exactly one domain. The Trade Service owns ingestion and P&L. The Intelligence Service owns all AI workflows. No service reaches into another's database. Communication is strictly via events or well-defined APIs.

### P4 — Fail Gracefully, Log Everything

LLM calls will fail. Kafka will lag. The system must handle partial failures without data loss. Every trade event is persisted before processing begins. Dead letter queues catch failed events. All LLM interactions are logged for debugging.

### P5 — Boring Infrastructure, Interesting Logic

Use well-understood, well-documented tools. No exotic databases. No cutting-edge frameworks that will break in six months. The innovation is in the domain logic and AI integration — not in the infrastructure choices.

### P6 — Embeddings Are Source of Truth for Search

No complex SQL queries for analytical questions. All queryable data is embedded and stored in pgvector. The RAG pipeline is the single interface for natural language questions. This is simpler and more powerful than building N bespoke analytics endpoints.

### P7 — Observable by Default

Every service exposes Prometheus metrics via Spring Boot Actuator. Grafana dashboards are set up from day one. If something breaks, you should know before you open the app.

---

## 7. Technology Stack

### Backend

| Layer | Technology | Reason |
|-------|-----------|--------|
| Language | Java 21 | LTS, virtual threads (Project Loom) available |
| Framework | Spring Boot 3.3 | Native AI libraries, familiar ecosystem |
| AI Library | Spring AI | First-class LLM + embedding + RAG support in Spring |
| Service Communication | REST (sync) + Redpanda (async) | Sync for queries, async for pipeline |
| Scheduling | Spring Batch | Nightly pattern detection jobs |
| Auth | Keycloak 24 | OAuth2/OIDC, Spring Security integration |
| API Gateway | Spring Cloud Gateway | Single entry point, JWT validation |

### Data

| Layer | Technology | Reason |
|-------|-----------|--------|
| Primary DB | PostgreSQL 16 | Relational, robust, free |
| Vector Store | pgvector extension | RAG embeddings in same DB, no extra infra |
| Cache | Redis | LLM response caching, rate limiting |
| Message Bus | Redpanda (local) | Kafka-compatible, single binary, zero ZooKeeper |

### AI

| Component | Technology | Reason |
|-----------|-----------|--------|
| LLM (dev) | Ollama + Llama 3.1 8B | Free, local, no API cost |
| LLM (prod) | Google Gemini 1.5 Flash | Free tier, capable, fast |
| Embeddings | Ollama nomic-embed-text | Free, local, 768-dim embeddings |
| Orchestration | Spring AI | Avoids LangChain4j complexity for this scope |

### Frontend

| Layer | Technology | Reason |
|-------|-----------|--------|
| Framework | React 18 + Vite | Fast, minimal setup |
| Styling | Tailwind CSS | Utility-first, no design system needed |
| State | Zustand | Lightweight, no Redux overhead |
| Charts | Recharts | Simple, React-native |

### Infrastructure

| Component | Technology | Reason |
|-----------|-----------|--------|
| Containerization | Docker + Docker Compose | Full local stack in one command |
| Monitoring | Prometheus + Grafana | Production-grade observability |
| Deployment | Railway free tier | Zero cost, Docker-native |

---

## 8. High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                      React Frontend                      │
│         Upload CSV │ View Debrief │ Chat Interface       │
└───────────────────────────┬─────────────────────────────┘
                            │ HTTPS
┌───────────────────────────▼─────────────────────────────┐
│                  Spring Cloud Gateway                    │
│              JWT Validation via Keycloak                 │
└──────────┬────────────────────────────────┬─────────────┘
           │                                │
┌──────────▼──────────┐          ┌──────────▼──────────────┐
│    Trade Service     │          │  Intelligence Service   │
│                      │          │                         │
│  - CSV Ingestion     │          │  - AI Debrief Engine   │
│  - P&L Computation   │          │  - RAG Query Handler   │
│  - Rule Engine       │          │  - Pattern Detection   │
│  - Position Tracker  │          │  - Embedding Pipeline  │
│                      │          │                         │
│  Publishes to:       │          │  Consumes from:         │
│  trade.ingested      │─────────▶│  trade.ingested        │
│  rule.violated       │          │  rule.violated         │
└──────────┬──────────┘          └──────────┬─────────────┘
           │                                │
           └───────────┬────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│                    Redpanda (Kafka)                      │
│  Topics: trade.ingested │ rule.violated │ trade.tagged  │
└─────────────────────────────────────────────────────────┘
           │
┌──────────▼──────────────────────────────────────────────┐
│              PostgreSQL 16 + pgvector                   │
│                                                          │
│  trades │ positions │ rules │ violations │ journal      │
│  embeddings (vector) │ pattern_reports │ debriefs       │
└──────────────────────────────────────────────────────────┘
           │
┌──────────▼──────────────────────────────────────────────┐
│              Redis Cache                                 │
│     LLM response cache │ session store │ rate limits    │
└─────────────────────────────────────────────────────────┘
```

---

## 9. Service Breakdown

### 9.1 Trade Service

**Responsibility:** Everything from raw CSV to a settled, queryable trade record with P&L.

**Key components:**

- `CsvIngestionController` — Accepts multipart CSV upload, parses rows, validates schema
- `TradeNormalizer` — Maps Zerodha CSV columns to internal `Trade` schema
- `DeduplicationFilter` — Checks trade hash against DB before publishing
- `PnlComputationEngine` — FIFO cost basis, realized/unrealized split, charge calculation
- `RuleEvaluationConsumer` — Kafka consumer on `trade.ingested`, evaluates all active rules per trade
- `NaturalLanguageRuleParser` — Calls LLM to convert plain English rule to structured `RuleSchema`
- `PositionTracker` — Maintains live position state, updated on each trade event

**Publishes:** `trade.ingested`, `rule.violated`  
**Consumes:** Nothing (upstream service)  
**Database tables:** `trades`, `positions`, `rules`, `rule_violations`

---

### 9.2 Intelligence Service

**Responsibility:** All AI workflows — debrief generation, RAG queries, pattern detection, embedding management.

**Key components:**

- `DebriefScheduler` — Triggers at 4 PM daily, fetches today's trades, constructs debrief prompt
- `DebriefGenerator` — Calls LLM with historical context + today's trades → structured debrief
- `DebriefResponseProcessor` — Kafka consumer on `debrief.responded`, calls LLM to extract tags from trader's response
- `EmbeddingPipeline` — Kafka consumer on `trade.ingested` + `trade.tagged`, embeds trade documents into pgvector
- `RagQueryHandler` — Accepts natural language query → vector search → LLM synthesis → answer
- `PatternDetectionJob` — Spring Batch job, runs nightly, computes behavioral statistics, calls LLM for narration
- `WeeklyReportGenerator` — Aggregates weekly patterns into a readable report, stores and embeds it

**Publishes:** `debrief.generated`, `pattern.detected`  
**Consumes:** `trade.ingested`, `rule.violated`, `trade.tagged`  
**Database tables:** `debriefs`, `journal_entries`, `pattern_reports`, `embeddings` (via pgvector)

---

### 9.3 Spring Cloud Gateway

**Responsibility:** Single entry point. Routes to Trade Service or Intelligence Service. Validates JWT tokens issued by Keycloak. Rate limiting via Redis.

No business logic lives here.

---

## 10. Data Model

### trades

```sql
CREATE TABLE trades (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trade_hash      VARCHAR(64) UNIQUE NOT NULL,  -- SHA-256 of raw row for dedup
    symbol          VARCHAR(50) NOT NULL,
    instrument_type VARCHAR(20) NOT NULL,          -- EQ, FUT, CE, PE
    trade_type      VARCHAR(10) NOT NULL,          -- BUY, SELL
    quantity        INTEGER NOT NULL,
    price           DECIMAL(12, 4) NOT NULL,
    trade_date      DATE NOT NULL,
    trade_time      TIMESTAMP NOT NULL,
    exchange        VARCHAR(10) NOT NULL,
    strategy_tag    VARCHAR(100),                  -- Set via AI journal extraction
    emotion_tag     VARCHAR(50),                   -- Set via AI journal extraction
    mistake_type    VARCHAR(100),                  -- Set via AI journal extraction
    realized_pnl    DECIMAL(12, 2),
    brokerage       DECIMAL(10, 4),
    stt             DECIMAL(10, 4),
    net_pnl         DECIMAL(12, 2),
    created_at      TIMESTAMP DEFAULT NOW()
);
```

### rules

```sql
CREATE TABLE rules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    raw_text        TEXT NOT NULL,                 -- "Never risk more than 1%..."
    parsed_schema   JSONB NOT NULL,                -- LLM-generated structured rule
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT NOW()
);
```

### rule_violations

```sql
CREATE TABLE rule_violations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_id         UUID REFERENCES rules(id),
    trade_id        UUID REFERENCES trades(id),
    violation_detail TEXT NOT NULL,
    occurred_at     TIMESTAMP NOT NULL
);
```

### debriefs

```sql
CREATE TABLE debriefs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    debrief_date    DATE NOT NULL,
    generated_text  TEXT NOT NULL,
    trader_response TEXT,
    response_at     TIMESTAMP,
    created_at      TIMESTAMP DEFAULT NOW()
);
```

### pattern_reports

```sql
CREATE TABLE pattern_reports (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_type     VARCHAR(50) NOT NULL,          -- WEEKLY, NIGHTLY
    period_start    DATE NOT NULL,
    period_end      DATE NOT NULL,
    raw_statistics  JSONB NOT NULL,
    narrative       TEXT NOT NULL,                 -- LLM-narrated plain English
    created_at      TIMESTAMP DEFAULT NOW()
);
```

### trade_embeddings (pgvector)

```sql
CREATE TABLE trade_embeddings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_type     VARCHAR(50) NOT NULL,          -- TRADE, JOURNAL, PATTERN_REPORT
    source_id       UUID NOT NULL,
    content         TEXT NOT NULL,                 -- Serialized text that was embedded
    embedding       vector(768) NOT NULL,
    created_at      TIMESTAMP DEFAULT NOW()
);

CREATE INDEX ON trade_embeddings USING ivfflat (embedding vector_cosine_ops);
```

---

## 11. AI Integration Design

### 11.1 Natural Language Rule Parsing

**Input:** `"Never risk more than 1% of capital on a single trade"`

**Process:** LLM is prompted with the rule text and instructed to return a structured JSON schema:

```json
{
  "rule_type": "POSITION_SIZE",
  "condition": "SINGLE_TRADE_RISK",
  "operator": "LESS_THAN_OR_EQUAL",
  "threshold": 0.01,
  "threshold_type": "PERCENTAGE_OF_CAPITAL",
  "action": "FLAG_VIOLATION"
}
```

The rule evaluator in Trade Service interprets this schema — no hardcoded rule types.

**Spring AI implementation:** `ChatClient` with structured output binding to a `RuleSchema` record.

---

### 11.2 AI Debrief Generation

**Trigger:** Scheduled at 4 PM daily by `DebriefScheduler`

**Context assembled:**
- Today's trades with P&L
- Active rule violations today
- Trader's historical statistics (win rate, avg size, time-of-day performance)
- Last 5 debriefs for continuity

**Prompt strategy:** System prompt establishes the AI as a trading coach that knows the trader's history. User prompt provides today's data. The AI is instructed to ask exactly 2-3 specific questions — no generic feedback.

**Output:** Structured debrief with `narrative` and `questions[]` fields, stored in `debriefs` table.

---

### 11.3 RAG Query Pipeline

```
User Query (natural language)
        ↓
Embed query using nomic-embed-text
        ↓
Vector similarity search in pgvector
(k=10 nearest documents across trades, journals, pattern reports)
        ↓
Retrieved context assembled into prompt
        ↓
LLM synthesizes answer with numbers
        ↓
Response returned to user
```

**Spring AI implementation:** `VectorStore` (pgvector), `QuestionAnswerAdvisor`, `ChatClient` — wired together in `RagQueryHandler`.

**Key design decision:** The RAG pipeline is the only query interface for analytical questions. There are no bespoke SQL analytics endpoints. This is more powerful and eliminates the need to anticipate every possible query upfront.

---

### 11.4 Behavioral Pattern Narration

The `PatternDetectionJob` (Spring Batch) computes raw statistics:

```json
{
  "revenge_trade_count": 8,
  "revenge_trade_pnl": -14200,
  "post_3pm_win_rate": 0.19,
  "oversize_after_loss_instances": 5
}
```

This JSON is passed to the LLM with the prompt: *"You are a trading coach. Based on these statistics from the past week, write a direct, specific pattern report in plain English. Include rupee amounts. Do not be gentle."*

The LLM's narrative output is stored in `pattern_reports` and embedded for future RAG queries.

---

## 12. Event Design

### Topics

| Topic | Publisher | Consumers | Payload |
|-------|-----------|-----------|---------|
| `trade.ingested` | Trade Service | Intelligence Service, Rule Engine | `TradeEvent` |
| `rule.violated` | Trade Service (Rule Engine) | Intelligence Service | `RuleViolationEvent` |
| `trade.tagged` | Intelligence Service | Trade Service (update tags), Embedding Pipeline | `TradeTagEvent` |
| `debrief.generated` | Intelligence Service | (Frontend polls REST) | `DebriefEvent` |

### TradeEvent Schema

```json
{
  "eventId": "uuid",
  "eventType": "TRADE_INGESTED",
  "tradeId": "uuid",
  "symbol": "NIFTY23NOV18000CE",
  "tradeType": "BUY",
  "quantity": 50,
  "price": 125.50,
  "tradeTime": "2024-11-15T10:32:00",
  "realizedPnl": null,
  "occurredAt": "2024-11-15T10:32:05"
}
```

### Consumer Groups

- `trade-service-rule-evaluator` — Rule Engine consumer in Trade Service
- `intelligence-service-embedder` — Embedding pipeline in Intelligence Service
- `intelligence-service-pattern-trigger` — Triggers incremental pattern analysis

---

## 13. API Design

All APIs are prefixed `/api/v1`. Auth header required: `Authorization: Bearer <JWT>`.

### Trade Service APIs

```
POST   /api/v1/trades/upload          Upload Zerodha CSV
POST   /api/v1/trades                 Create single trade manually
GET    /api/v1/trades                 List trades (date range, symbol, strategy filters)
GET    /api/v1/trades/{id}            Get single trade with P&L detail
GET    /api/v1/positions              Get current open positions
GET    /api/v1/pnl/summary            Aggregate P&L by period
POST   /api/v1/rules                  Create rule (natural language input)
GET    /api/v1/rules                  List all rules with compliance rate
GET    /api/v1/rules/violations       List violations (date range)
```

### Intelligence Service APIs

```
GET    /api/v1/debrief/today          Get today's generated debrief
POST   /api/v1/debrief/{id}/respond   Submit trader response to debrief
GET    /api/v1/patterns/weekly        Get latest weekly pattern report
POST   /api/v1/query                  Natural language query over trade history
GET    /api/v1/query/history          Get past query sessions
```

---

## 14. Infrastructure & Deployment

### Local Development Stack (Docker Compose)

```yaml
services:
  keycloak:         # Auth — port 8080
  redpanda:         # Message bus — port 9092
  postgres:         # Database — port 5432
  redis:            # Cache — port 6379
  ollama:           # LLM runtime — port 11434
  trade-service:    # Port 8081
  intelligence-service: # Port 8082
  gateway:          # Port 8000
  prometheus:       # Metrics — port 9090
  grafana:          # Dashboards — port 3001
  frontend:         # React — port 3000
```

All services start with: `docker-compose up -d`

### Monitoring

- Spring Boot Actuator exposes `/actuator/prometheus` on each service
- Key metrics: trade ingestion rate, LLM latency, rule evaluation time, embedding pipeline lag
- Grafana dashboard per service — set up from day one, not as an afterthought

### Zero-Cost Deployment Path

| Component | Local | Production (free) |
|-----------|-------|-------------------|
| LLM | Ollama | Gemini 1.5 Flash free tier |
| Database | Local Postgres | Supabase free (500MB) |
| Message Bus | Redpanda | Redis Streams on Railway |
| Cache | Local Redis | Redis Cloud free (30MB) |
| Auth | Keycloak | Auth0 free tier |
| Services | Docker Compose | Railway free tier |

---

## 15. Build Phases

### Phase 1 — Foundation (Weeks 1–2)

- Docker Compose with all infrastructure services
- Keycloak setup + Spring Security integration
- Trade Service: CSV ingestion, normalization, deduplication
- P&L computation engine (FIFO, charges)
- Redpanda event publishing on trade ingestion
- Basic REST APIs for trades and P&L

**Milestone:** Upload a Zerodha CSV, see trades and P&L via API.

---

### Phase 2 — Rule Engine (Week 3–4)

- Natural language rule creation endpoint
- LLM integration for rule parsing (Spring AI + Ollama)
- Rule evaluation consumer on `trade.ingested`
- Rule violation storage and flagging
- Rule compliance rate computation

**Milestone:** Create a rule in plain English, upload trades, see violations flagged.

---

### Phase 3 — AI Debrief + Journaling (Week 5–6)

- Embedding pipeline (nomic-embed-text via Ollama, pgvector storage)
- Debrief generation scheduler (4 PM daily)
- Historical context assembly for debrief prompt
- Trader response endpoint + AI tag extraction
- Tags written back to trade records via `trade.tagged` event

**Milestone:** End-of-day debrief generated, respond to it, see tags appear on trades.

---

### Phase 4 — Pattern Detection + RAG (Week 7–8)

- Spring Batch nightly job for pattern detection
- Statistical pattern algorithms (revenge trade, overtrading, time-of-day)
- LLM narration of pattern statistics
- RAG query handler (vector search + LLM synthesis)
- Chat interface in frontend
- Prometheus + Grafana dashboards finalized

**Milestone:** Ask "how much have I lost to revenge trades?" and get a correct, cited answer.

---

## 16. Open Questions & Risks

| # | Question / Risk | Mitigation |
|---|-----------------|------------|
| 1 | Ollama LLM quality for complex rule parsing | Test with Llama 3.1 8B early; fall back to Gemini free tier if needed |
| 2 | pgvector retrieval relevance for trade queries | Tune embedding text format; include rich metadata in embedded content |
| 3 | Redpanda stability in Docker | Use Redis Streams as fallback if Redpanda proves unstable on dev machine |
| 4 | Spring Batch job runtime on large history | Add incremental processing — only process trades since last job run |
| 5 | Keycloak memory footprint locally | Cap at 512MB in Docker Compose; switch to Auth0 if needed |
| 6 | LLM hallucinating P&L numbers in RAG | Ground all numerical answers in SQL aggregates; LLM narrates, never computes |
| 7 | CSV format changes between Zerodha versions | Version-detect CSV headers; fail loudly on unknown format |

---

*This document is a living reference. Update as implementation decisions are made.*
