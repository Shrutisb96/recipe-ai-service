# 🍳 Recipe AI

> An intelligent recipe suggestion platform that uses AI to recommend recipes based on your available ingredients, calorie goals, and dietary preferences — built with Java Spring Boot and powered by Claude AI.

---

## Table of Contents

- [Overview](#overview)
- [Functional Requirements](#functional-requirements)
- [Non-Functional Requirements](#non-functional-requirements)
- [Tech Stack](#tech-stack)
- [System Architecture](#system-architecture)
- [API Reference](#api-reference)
- [Database Schema](#database-schema)
- [Caching Strategy](#caching-strategy)
- [Observability Stack](#observability-stack)
- [TPS & Scalability](#tps--scalability)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Running Tests](#running-tests)

---

## Overview

Recipe AI solves a simple problem: you open your fridge, see a handful of ingredients, and have no idea what to cook. You tell the app what you have and what your calorie goal is — it asks Claude AI and returns recipe suggestions in seconds, including what extra ingredients you'd need.

The app is designed for the general public with personal accounts, calorie tracking, saved favourites, and recurring meal schedules.

---

## Functional Requirements

### FR-1: Recipe Suggestion
- Accept a list of available ingredients and return AI-generated recipe suggestions
- Match recipes to a target calorie count within ±10%
- Support dietary filters: vegetarian, vegan, gluten-free, dairy-free, keto
- Identify and list missing ingredients for each suggested recipe
- Return a configurable number of suggestions (default: 3, max: 5 per request, 10 fetched internally for pagination)
- Each recipe includes: name, ingredients, missing ingredients, steps, estimated calories, prep time, and tags

### FR-2: User Accounts & Authentication
- Register with email and password
- Login and receive a JWT token valid for 24 hours
- All data scoped to the authenticated user
- Logout and invalidate token

### FR-3: Favourite Recipes
- Save a suggested recipe to personal favourites
- View all saved recipes
- Delete a recipe from favourites

### FR-4: Daily Calorie Intake Tracker
- Log a recipe as consumed on a specific date
- View total calories consumed for the current day
- View calorie history for the past 7 days
- Warning triggered when daily intake exceeds the user's calorie goal

### FR-5: Recurring Recipe List
- Mark a recipe as recurring (daily or weekly on a specific day)
- Surface recurring recipes on their scheduled day
- Advance the schedule after a recipe is acknowledged
- Remove a recipe from the recurring list

### FR-6: Pagination
- Internally fetch 10 recipes per AI call
- Return 5 at a time to the client
- Serve subsequent pages from cache — no extra AI calls
- Fresh AI call only when the cache batch is exhausted

### FR-7: AI Fallback
- If the Claude API is unavailable, return a curated set of pre-stored fallback recipes from the database
- Fallback recipes honour the dietary filter provided in the request

---

## Non-Functional Requirements

### NFR-1: Performance
| Metric | Target |
|--------|--------|
| Recipe suggestion p95 latency | < 3 seconds |
| Database read queries | < 500ms |
| Cache hit response time | < 100ms |

### NFR-2: Scalability
- Supports up to 1,000 concurrent registered users without degradation on a single-node deployment
- Database indexed on `user_id`, `created_at`, recipe `name`
- AI API calls rate-limited to 10 requests per user per minute
- At 1M+ users: 20+ Spring Boot instances behind a load balancer, PostgreSQL read replicas, Redis cluster

### NFR-3: Reliability
- 99.5% uptime (excluding planned maintenance)
- Claude API retried up to 2 times with 1-second exponential backoff before falling back
- Resilience4j circuit breaker opens after 5 consecutive AI failures, pausing calls for 30 seconds
- User data (favourites, calorie logs) never lost due to transient errors

### NFR-4: Security
- All endpoints except `/v1/auth/register` and `/v1/auth/login` require a valid JWT token
- Passwords hashed with BCrypt — never stored in plain text
- Anthropic API key and JWT secret stored as environment variables — never hardcoded
- User input (ingredient lists) sanitised before inclusion in AI prompts
- HTTPS enforced in production

### NFR-5: API Design
- Consistent JSON response envelope: `data`, `error`, `message` fields
- Validation errors return HTTP 400 with field-level error messages
- REST conventions followed throughout
- OpenAPI/Swagger UI available at `/swagger-ui.html`

### NFR-6: Maintainability
- Standard Spring Boot layered architecture: Controller → Service → Repository
- Business logic unit test coverage ≥ 70%
- Integration tests use Testcontainers (real Postgres) — no mocked databases
- Fully containerised with Docker, runnable via `docker compose up`

---

## Tech Stack

### Core Backend

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 21 (LTS) | Primary language — uses virtual threads (Project Loom) |
| **Spring Boot** | 3.3.x | Application framework — REST API, dependency injection, auto-configuration |
| **Spring Data JPA** | — | ORM layer — maps Java entities to PostgreSQL tables |
| **Spring Security** | — | Authentication filter chain, JWT validation, endpoint protection |
| **Hibernate** | — | JPA implementation — SQL generation, schema management (`ddl-auto=update`) |
| **Lombok** | — | Eliminates boilerplate (`@Data`, `@RequiredArgsConstructor`, etc.) |
| **Maven** | 3.9.x | Build tool, dependency management, test runner |

### AI Integration

| Technology | Purpose |
|------------|---------|
| **Claude API (Anthropic)** | Generates recipe suggestions from a structured prompt. Model: `claude-sonnet-4-20250514`. Called via `RestTemplate` over HTTPS. Response parsed from `content[0].text`. |
| **Prompt engineering** | Structured prompt with strict JSON schema instruction, dietary constraints, calorie targets, and explicit "no markdown" directive to ensure parseable output every time. |
| **Temperature: 0.5** | Balances variety with consistency. Lower = same recipes every call. Higher = creative but less predictable. |

### Data Layer

| Technology | Purpose |
|------------|---------|
| **PostgreSQL 16** | Primary relational database. Stores users, favourites, calorie logs, recurring recipes, suggestion logs. |
| **Redis 7** | In-memory cache for AI recipe batches. TTL-based expiry (2 hours default). Key = MD5 hash of normalised ingredient combo + calorie + preference. Handles ~100,000 ops/sec — zero bottleneck at any realistic user count. |
| **PgBouncer** | Connection pooler for PostgreSQL at scale. Sits between Spring Boot and Postgres, multiplexes connections. Critical at 10K+ users when multiple Spring Boot instances create hundreds of DB connections. |

### Security

| Technology | Purpose |
|------------|---------|
| **JWT (JSON Web Tokens)** | Stateless authentication. Token issued on login, validated on every protected request via Spring Security filter. Expiry: 24 hours. |
| **BCrypt** | Password hashing algorithm. Industry standard, adaptive cost factor, resistant to brute force. Never store plain text passwords. |
| **Spring Security** | Configures the security filter chain, protects endpoints, integrates JWT validation. |

### Resilience

| Technology | Purpose |
|------------|---------|
| **Resilience4j** | Circuit breaker around Claude API calls. Opens after 5 failures, prevents cascading failure, closes automatically after 30s. Also provides retry logic with exponential backoff. |
| **RestTemplate timeouts** | `ConnectTimeout=5s`, `ReadTimeout=45s` on Claude API calls. Prevents threads hanging indefinitely on a slow AI response. |

### Observability

| Technology | Purpose |
|------------|---------|
| **Spring Boot Actuator** | Exposes `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` — health checks and metrics endpoint out of the box. |
| **Prometheus** | Scrapes metrics from `/actuator/prometheus` every 15 seconds. Stores time-series data: request rate, latency percentiles, JVM heap, cache hit rate. |
| **Grafana** | Visualises Prometheus metrics as dashboards. Live graphs for TPS, error rate, Claude API call rate, cache hit %, response latency. |
| **Elasticsearch** | Indexes structured application logs. Full-text searchable, supports complex queries across millions of log entries. |
| **Logstash** | Collects logs from Spring Boot (via Logback), transforms them, ships to Elasticsearch. |
| **Kibana** | Log explorer UI on top of Elasticsearch. Use it to trace specific requests, debug errors, find top ingredient combos. |
| **Logback (SLF4J)** | Spring Boot's default logging framework. Configured to output structured JSON logs for ELK ingestion. |

### Infrastructure & DevOps

| Technology | Purpose |
|------------|---------|
| **Docker** | Containerises the Spring Boot app for consistent environments across dev, test, and production. |
| **Docker Compose** | Runs the full local stack: app + Postgres + Redis + Elasticsearch + Kibana + Prometheus + Grafana in one command. |
| **GitHub Actions** | CI pipeline — builds, tests, and verifies the app on every push and pull request. Runs integration tests with a real Postgres container. |
| **Kubernetes** *(future)* | Container orchestration for 10K+ users. Auto-scales Spring Boot pods based on CPU/memory. |

### Testing

| Technology | Purpose |
|------------|---------|
| **JUnit 5** | Unit testing framework. |
| **Mockito** | Mocks dependencies in unit tests (`AiGatewayService`, `RestTemplate`, repositories). |
| **Testcontainers** | Spins up a real PostgreSQL Docker container during integration tests. No mocked databases. |
| **Spring Boot Test** | `@WebMvcTest` for controller layer tests, `@SpringBootTest` for full integration tests. |

---

## System Architecture

```
                        ┌─────────────────────────────────┐
                        │           Client                 │
                        │  (Mobile App / Postman / Web)    │
                        └──────────────┬──────────────────┘
                                       │ HTTPS
                        ┌──────────────▼──────────────────┐
                        │         Spring Boot              │
                        │  ┌─────────────────────────┐    │
                        │  │    RecipeController      │    │
                        │  │    FavouritesController  │    │
                        │  │    CalorieController     │    │
                        │  │    RecurringController   │    │
                        │  │    AuthController        │    │
                        │  └───────────┬─────────────┘    │
                        │  ┌───────────▼─────────────┐    │
                        │  │    RecipeService         │    │
                        │  │    AiGatewayService      │    │
                        │  │    CalorieService        │    │
                        │  │    RecurringService      │    │
                        │  └──┬──────────────────┬───┘    │
                        └─────│──────────────────│────────┘
                              │                  │
               ┌──────────────▼──┐    ┌─────────▼──────────────┐
               │   Redis Cache   │    │    Claude API           │
               │  recipe batches │    │  (Anthropic)            │
               │  TTL: 2 hours   │    │  claude-sonnet-4        │
               └─────────────────┘    └────────────────────────┘
                              │
               ┌──────────────▼──────────────────┐
               │           PostgreSQL             │
               │  users · favourite_recipes       │
               │  calorie_logs · recurring        │
               │  suggestion_logs                 │
               └─────────────────────────────────┘
                              │
          ┌───────────────────▼──────────────────────┐
          │           Observability Stack             │
          │  Prometheus → Grafana  (metrics/alerts)  │
          │  Logstash → Elasticsearch → Kibana (logs)│
          └──────────────────────────────────────────┘
```

---

## API Reference

### Auth

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/v1/auth/register` | No | Create user account |
| `POST` | `/v1/auth/login` | No | Login, returns JWT |

### User

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/v1/user` | Yes | Get logged-in user details |
| `PUT` | `/v1/user` | Yes | Update user details / calorie goal |

### Recipes

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/v1/recipes/suggest` | Yes | Get AI recipe suggestions |

**Request body:**
```json
{
  "ingredients": ["eggs", "tomatoes", "cheese"],
  "targetCalories": 500,
  "dietaryPreference": "vegetarian",
  "cuisineType": "italian",
  "numberOfSuggestions": 3
}
```

**Response:**
```json
[
  {
    "name": "Shakshuka",
    "ingredients": ["eggs", "tomatoes", "cheese"],
    "missingIngredients": ["cumin", "paprika"],
    "steps": ["Heat oil in a pan...", "Add tomatoes..."],
    "estimatedCalories": 480,
    "prepTimeMinutes": 20,
    "tags": ["vegetarian", "high-protein"]
  }
]
```

### Favourites

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/v1/recipes/favourites` | Yes | List saved favourites |
| `POST` | `/v1/recipes/favourites` | Yes | Save a recipe to favourites |
| `DELETE` | `/v1/recipes/favourites/{id}` | Yes | Remove a favourite |

### Calorie Tracker

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/v1/calories` | Yes | Get 7-day calorie history |
| `POST` | `/v1/calories` | Yes | Log a calorie entry |
| `DELETE` | `/v1/calories/{id}` | Yes | Delete a logged entry |

### Recurring Recipes

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/v1/recipes/recurring` | Yes | List recurring recipes |
| `POST` | `/v1/recipes/recurring` | Yes | Add a recurring recipe |
| `DELETE` | `/v1/recipes/recurring/{id}` | Yes | Remove a recurring recipe |

---

## Database Schema

```sql
-- Users
CREATE TABLE users (
  id                 BIGSERIAL PRIMARY KEY,
  email              VARCHAR(255) UNIQUE NOT NULL,
  password_hash      VARCHAR(255) NOT NULL,
  daily_calorie_goal INT DEFAULT 2000,
  created_at         TIMESTAMP DEFAULT NOW()
);

-- Favourites
CREATE TABLE favourite_recipes (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT REFERENCES users(id) ON DELETE CASCADE,
  name        VARCHAR(255) NOT NULL,
  recipe_json TEXT NOT NULL,
  created_at  TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_fav_user ON favourite_recipes(user_id);

-- Calorie logs
CREATE TABLE calorie_logs (
  id                 BIGSERIAL PRIMARY KEY,
  user_id            BIGINT REFERENCES users(id) ON DELETE CASCADE,
  recipe_name        VARCHAR(255) NOT NULL,
  base_calories      INT NOT NULL,
  portion_multiplier DECIMAL(3,1) NOT NULL DEFAULT 1.0,
  calories           INT NOT NULL,
  logged_date        DATE NOT NULL DEFAULT CURRENT_DATE,
  logged_at          TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_cal_user_date ON calorie_logs(user_id, logged_date DESC);

-- Recurring recipes
CREATE TABLE recurring_recipes (
  id            BIGSERIAL PRIMARY KEY,
  user_id       BIGINT REFERENCES users(id) ON DELETE CASCADE,
  recipe_json   TEXT NOT NULL,
  frequency     VARCHAR(20) NOT NULL,  -- DAILY or WEEKLY
  day_of_week   INT,                   -- 1=Mon..7=Sun, null if DAILY
  next_due_date DATE NOT NULL
);
CREATE INDEX idx_rec_user ON recurring_recipes(user_id);
CREATE INDEX idx_rec_due  ON recurring_recipes(next_due_date);

-- Suggestion logs (for cache pre-warming analytics)
CREATE TABLE suggestion_logs (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT,
  ingredients TEXT NOT NULL,  -- sorted, comma-separated
  calories    INT,
  preference  VARCHAR(50),
  cache_hit   BOOLEAN DEFAULT FALSE,
  created_at  TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_slog_date ON suggestion_logs(created_at DESC);
```

---

## Caching Strategy

Recipe suggestions are cached in Redis to minimise Claude API calls and reduce latency.

**Cache key** — MD5 hash of the normalised request:
```java
String cacheKey = DigestUtils.md5Hex(
    userId + "|" +
    ingredients.stream().sorted().collect(Collectors.joining(",")) + "|" +
    targetCalories + "|" +
    dietaryPreference
);
```

**Flow:**
1. Check Redis for `cacheKey` — if hit, return immediately (< 100ms)
2. If miss — call Claude API, store 10 recipes in Redis with 2-hour TTL, return first 5
3. On "show more" — read next 5 from Redis, no AI call

**Cache pre-warming** — runs nightly at 3am:
```
Top 100 ingredient combos (from suggestion_logs, last 7 days)
→ Call Claude for each (if not already cached)
→ Store in Redis with 24-hour TTL
→ Next day: those requests served instantly, zero AI cost
```

---

## Observability Stack

| Tool | URL (local) | What to look at |
|------|-------------|-----------------|
| **Grafana** | `http://localhost:3000` | TPS, cache hit rate, latency, error rate, Claude API call rate |
| **Kibana** | `http://localhost:5601` | Raw logs, error traces, request debugging, top ingredient combos |
| **Prometheus** | `http://localhost:9090` | Raw metrics, scrape targets |
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` | API docs and manual testing |
| **Actuator** | `http://localhost:8080/actuator/health` | App health, metrics |

---

## TPS & Scalability

| Users | Peak Concurrent | Total TPS | Claude calls/sec | Spring Boot nodes | Postgres |
|-------|----------------|-----------|-----------------|-------------------|----------|
| 1,000 | 100 | 1.3 | 0.2 | 1 | Single node |
| 10,000 | 1,000 | 13 | 2 | 1–2 | Single + read replica |
| 100,000 | 10,000 | 133 | 20 | 5–8 | Primary + 2 replicas + PgBouncer |
| 1,000,000 | 100,000 | 1,333 | 160 | 20+ on Kubernetes | Primary + 3 replicas + PgBouncer |

> **Note:** Claude API is the primary cost driver at scale. Mitigations: aggressive caching (target 85%+ hit rate), per-user daily quotas, tiered model strategy (Haiku for simple requests, Sonnet for complex ones).

---

## Getting Started

### Prerequisites

- Java 21
- Docker Desktop
- An Anthropic API key from [console.anthropic.com](https://console.anthropic.com)

### 1. Clone the repository

```bash
git clone https://github.com/your-username/recipe-ai.git
cd recipe-ai
```

### 2. Set environment variables

```bash
# Mac / Linux
export AI_API_KEY=sk-ant-your-key-here
export JWT_SECRET=your-256-bit-secret-here

# Windows PowerShell
$env:AI_API_KEY="sk-ant-your-key-here"
$env:JWT_SECRET="your-256-bit-secret-here"
```

### 3. Start all services

```bash
docker compose up -d
```

This starts: PostgreSQL, Redis, Elasticsearch, Kibana, Prometheus, Grafana.

### 4. Run the application

```bash
./mvnw spring-boot:run
```

The API is available at `http://localhost:8080`.

### 5. Verify everything is running

```bash
curl http://localhost:8080/actuator/health
```

Should return `{"status":"UP"}`.

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `AI_API_KEY` | Yes | Anthropic API key |
| `JWT_SECRET` | Yes | 256-bit secret for signing JWT tokens |
| `SPRING_DATASOURCE_URL` | No | Defaults to `jdbc:postgresql://localhost:5432/recipedb` |
| `SPRING_DATASOURCE_USERNAME` | No | Defaults to `recipe` |
| `SPRING_DATASOURCE_PASSWORD` | No | Defaults to `secret` |
| `SPRING_DATA_REDIS_HOST` | No | Defaults to `localhost` |
| `AI_MODEL` | No | Defaults to `claude-sonnet-4-20250514` |
| `AI_MAX_TOKENS` | No | Defaults to `3000` (supports 10-recipe batches) |
| `AI_TIMEOUT_SECONDS` | No | Defaults to `45` |

---

## Running Tests

```bash
# All tests (unit + integration)
./mvnw test

# Unit tests only
./mvnw test -Dgroups=unit

# Integration tests only (requires Docker for Testcontainers)
./mvnw test -Dgroups=integration

# With coverage report
./mvnw verify
# Report at: target/site/jacoco/index.html
```

---

## Out of Scope (v1)

- Social features (sharing recipes with other users)
- Detailed nutrition breakdown beyond calories (protein, fat, carbs)
- Voice input for ingredients
- Third-party login (Google, Apple Sign-In)
- Payment or subscription tiers
- Push notifications for recurring recipes

---

## License

MIT
