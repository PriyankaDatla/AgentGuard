# AgentGuard — The Financial Firewall for AI Agents

AgentGuard is a backend boundary between an AI purchasing agent and payment systems. A user submits a natural-language purchase request; the Groq LLM extracts a structured `PurchaseIntent` from it. A deterministic policy engine then independently evaluates that intent against the user's configured spending limits and purchase policies, and returns one of three decisions: `APPROVED`, `BLOCKED`, or `MANUAL_REVIEW`. Only transactions that reach `APPROVED` status may proceed to create a Razorpay payment order. The LLM has no authority to approve, block, or execute payments, it only reads the purchase request and produces structured data.

---

## Problem Statement

AI agents making autonomous purchasing decisions are a significant financial risk. An LLM that can call payment APIs directly can be manipulated through prompt injection, hallucination, or misconfiguration into spending money it should not. There is no standard enforcement layer between an AI's purchasing intent and the actual execution of a payment.

## Solution

AgentGuard enforces a strict separation of concerns:

- The **LLM layer** handles natural language, nothing else.
- The **policy engine** handles financial authority deterministically, with no ML involved.
- The **payment layer** executes only when the policy engine has explicitly approved a transaction.

No path exists from user input to payment that bypasses the policy engine.

---

## Architecture

```
User submits natural-language purchase request
               │
               ▼
         Groq LLM (openai/gpt-oss-120b)
         Intent extraction only
         Outputs: category, preferredBrand, maxAmount, requiresCategoryMatch
               │
               ▼
         Structured PurchaseIntent (untrusted input)
               │
               ▼
         Deterministic Policy Engine
         ├─ VALID_TRANSACTION check
         ├─ USER_SPENDING_LIMIT check
         ├─ INTENT_MAX_AMOUNT check
         ├─ CATEGORY_MATCH check
         ├─ AMOUNT_LIMIT check (strictest matching policy)
         ├─ HOURLY_SPENDING_LIMIT check
         ├─ DUPLICATE_TRANSACTION check
         └─ REQUIRES_APPROVAL check
               │
       ┌───────┴────────────────┐
       ▼                        ▼                  ▼
   APPROVED                 BLOCKED          MANUAL_REVIEW
       │
       ▼
  POST /api/payments/create
  Razorpay order created (INR, paise conversion)
  Transaction status → PAYMENT_CREATED
```

> **The LLM can understand the purchase, but it cannot authorize the payment.**

---

## Key Features

- Natural-language purchase evaluation via Groq LLM
- Deterministic policy engine with 8 named, inspectable policy checks
- Per-user spending limits (total and hourly)
- Per-user, per-category purchase policies with configurable amount caps
- Duplicate transaction detection (10-minute window)
- Transparent risk scoring (0–100) returned with every decision
- Full audit trail per transaction (created → evaluated → approved/blocked/manual review → payment)
- Razorpay payment order creation for approved transactions — idempotent
- React dashboard: evaluate purchases, manage policies, view transaction history, create payment orders
- User management: create users, switch between users, per-user policy isolation enforced in UI and backend

---

## Demo Scenarios

All scenarios use `POST /api/agent/evaluate` (Groq + policy engine) or `POST /api/transactions/evaluate` (direct, no LLM).

### 1. Valid purchase → APPROVED

User has an `electronics` policy (max ₹25,000) and a spending limit of ₹50,000.

```json
POST /api/agent/evaluate
{
  "userId": 1,
  "merchant": "Croma",
  "request": "Buy Sony WH-1000XM5 headphones under ₹25,000",
  "amount": 20500
}
```

All 8 policy checks pass. Response includes `decision: "APPROVED"` and a Razorpay order can be created immediately via `POST /api/payments/create`.

### 2. Amount exceeds policy limit → BLOCKED

Same user and policy (max ₹25,000), amount over the cap.

```json
{
  "userId": 1,
  "merchant": "Croma",
  "request": "Buy Sony WH-1000XM5 headphones under ₹35,000",
  "amount": 30000
}
```

`AMOUNT_LIMIT` check fails. No payment order possible.

### 3. No matching policy → BLOCKED

User has only an `electronics` policy. Request is for a different category.

```json
{
  "userId": 1,
  "merchant": "Nike",
  "request": "Buy running shoes under ₹5,000",
  "amount": 4500
}
```

`CATEGORY_MATCH` check fails. Purchase blocked regardless of amount.

### 4. Duplicate transaction → MANUAL_REVIEW

Submit the same merchant, category, and amount twice within 10 minutes. The second request returns `decision: "MANUAL_REVIEW"`. No payment order possible.

### 5. Blocked transaction cannot create a payment order

```json
POST /api/payments/create
{ "transactionId": <id_of_blocked_transaction> }
```

Returns HTTP 422 `"Only approved transactions can create a payment order"`.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Backend framework | Spring Boot 3.5.0 |
| Persistence | Spring Data JPA + Hibernate |
| Database | MySQL 8 |
| LLM provider | Groq (OpenAI-compatible API) |
| Payment | Razorpay Java SDK 1.4.10 |
| Build tool | Maven |
| Frontend framework | React 18.3.1 |
| Frontend build | Vite 5.4.19 |
| HTTP client (backend) | Spring `RestClient` |
| Validation | Jakarta Bean Validation |
| Testing | JUnit 5, Mockito, Spring Boot Test |

---

## Setup and Run

### Prerequisites

- Java 21+
- Maven 3.9+
- MySQL 8 running on port 3306
- Node.js 18+ and npm (for the frontend)
- A Groq API key
- Razorpay test API keys (optional — backend starts without them; payment orders return 502 until configured)

### MySQL

```sql
CREATE DATABASE agentguard;
CREATE USER 'agentguard_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON agentguard.* TO 'agentguard_user'@'localhost';
```

Hibernate creates and updates the schema automatically on startup (`ddl-auto: update`).

### Environment Variables

```
DB_URL=jdbc:mysql://localhost:3306/agentguard?createDatabaseIfNotExist=true&serverTimezone=UTC
DB_USERNAME=your_username
DB_PASSWORD=your_password

GROQ_API_KEY=your_groq_api_key
GROQ_MODEL=openai/gpt-oss-120b
GROQ_BASE_URL=https://api.groq.com/openai/v1

RAZORPAY_KEY_ID=your_razorpay_test_key_id
RAZORPAY_KEY_SECRET=your_razorpay_test_key_secret
```

`GROQ_MODEL` and `GROQ_BASE_URL` are optional; the defaults shown above are used when omitted.

### Start the Backend

```powershell
# Windows PowerShell
$env:JAVA_HOME    = "C:\path\to\jdk-21"
$env:DB_URL       = "jdbc:mysql://localhost:3306/agentguard?serverTimezone=UTC"
$env:DB_USERNAME  = "your_username"
$env:DB_PASSWORD  = "your_password"
$env:GROQ_API_KEY = "your_groq_api_key"
$env:RAZORPAY_KEY_ID     = "your_test_key_id"
$env:RAZORPAY_KEY_SECRET = "your_test_key_secret"
mvn spring-boot:run
```

Backend listens on **http://localhost:8081**.

### Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on **http://localhost:5173** with all `/api/*` requests proxied to the backend.

### Run Tests

```bash
mvn test
```

### Build Frontend for Production

```bash
cd frontend
npm run build
```

---

## Razorpay AI Buildathon

AgentGuard is submitted to the **Open Track**.

The project demonstrates that AI agents and payment systems can coexist safely when the architecture enforces a strict, unbypassable boundary between them. The core insight: AI is useful for understanding intent, but must never be trusted with financial authority. AgentGuard makes this boundary structural — not just a guideline — by ensuring the only code path to Razorpay runs through a deterministic engine that has no awareness of the LLM and cannot be influenced by it.

The Razorpay SDK is used for its intended purpose: creating payment orders in INR, with exact paise arithmetic, idempotent order creation, and full audit trails.

---

## Security Boundary

> **The LLM can understand the purchase, but it cannot authorize the payment.**

The LLM is invoked once per request to extract structured intent from natural language. It operates under a strict system prompt that prohibits it from authorising transactions, modifying policies, approving payments, or calling APIs. The `PurchaseIntent` it produces is treated as untrusted input by the policy engine. Every financial decision is made by deterministic Java code.

- The LLM **cannot** change spending limits.
- The LLM **cannot** modify or bypass purchase policies.
- The LLM **cannot** approve or reject a transaction.
- The LLM **cannot** call Razorpay.
- Razorpay is called only after the policy engine returns `APPROVED`.

---

## AI Component

| Property | Value |
|---|---|
| Provider | Groq |
| Default model | `openai/gpt-oss-120b` (overridable via `GROQ_MODEL`) |
| API | OpenAI-compatible `/v1/chat/completions` |
| Response format | `json_schema` with strict mode |
| Connection timeout | 5 000 ms |
| Read timeout | 15 000 ms |
| Output schema | `category` (string), `preferredBrand` (string or null), `maxAmount` (number > 0), `requiresCategoryMatch` (boolean) |

The LLM is instructed to return only the four permitted fields. If either `category` or `maxAmount` is ambiguous it returns no result, which fails safely. The response is validated server-side before the `PurchaseIntent` is accepted.

---

## Policy & Risk Engine

All checks are implemented in `DeterministicPolicyEngine`. Each check produces a named `PolicyCheck` record with a pass/fail flag and a human-readable explanation returned to the caller.

| Check | Rule | Result on failure |
|---|---|---|
| `VALID_TRANSACTION` | Amount > 0, merchant non-blank, intent fields present | BLOCKED |
| `USER_SPENDING_LIMIT` | `requestedAmount ≤ user.spendingLimit` | BLOCKED |
| `INTENT_MAX_AMOUNT` | `requestedAmount ≤ intent.maxAmount` | BLOCKED |
| `CATEGORY_MATCH` | A user-owned active policy exists for `intent.category` | BLOCKED |
| `AMOUNT_LIMIT` | `requestedAmount ≤ strictest matching policy maxAmount` | BLOCKED |
| `HOURLY_SPENDING_LIMIT` | `hourlySpending + requestedAmount ≤ user.hourlySpendingLimit` | BLOCKED |
| `DUPLICATE_TRANSACTION` | No identical (merchant + category + amount) transaction in the last 10 minutes | MANUAL_REVIEW |
| `REQUIRES_APPROVAL` | Matching policy does not require manual approval | MANUAL_REVIEW |

**Risk score** (0–100) is computed transparently:
- +20 if amount ≥ 80% of the user's spending limit
- +20 if projected hourly spend ≥ 80% of the hourly limit
- +40 if a duplicate transaction was detected

The hourly spending sum considers transactions in `APPROVED`, `PAYMENT_CREATED`, and `COMPLETED` statuses from the last 60 minutes.

---

## User-Specific Policy Isolation

Policies are stored per user (`userId` foreign key on `purchase_policies`). When a transaction is evaluated, the policy engine receives only the active policies belonging to the requesting user. A user with no policy for a given category will always receive `BLOCKED` for that category, regardless of what other users' policies allow. The frontend enforces this by always sending the active user's ID in every API call.

---

## Razorpay Integration

Payment orders are created using the official Razorpay Java SDK (`razorpay-java:1.4.10`).

1. Client calls `POST /api/payments/create` with a `transactionId`.
2. The service loads the transaction using a pessimistic write lock (`SELECT ... FOR UPDATE`) to prevent duplicate order creation.
3. If the transaction already has a `razorpayOrderId`, the existing order is returned without calling Razorpay again (idempotent).
4. If the transaction status is not `APPROVED`, a `PaymentNotAllowedException` is thrown — **BLOCKED and MANUAL_REVIEW transactions cannot create payment orders**.
5. The amount is converted from rupees to paise with exact arithmetic; amounts with more than two decimal places are rejected.
6. On success: `razorpayOrderId` is stored, status becomes `PAYMENT_CREATED`, and a `PAYMENT_CREATED` audit event is written.
7. On failure: status becomes `PAYMENT_FAILED`, a `PAYMENT_FAILED` audit event is written, and no partial state is left.

---

## Backend Architecture

```
com.agentguard
├── controller      REST endpoints — thin; delegate to services
├── service         Business logic — UserService, PolicyService,
│                   PolicyEvaluationService, AgentEvaluationService,
│                   TransactionService, AuditService
├── policy          DeterministicPolicyEngine, PolicyEvaluationContext,
│                   PolicyDecision, PurchaseIntent (pure, no DB access)
├── intent          GroqIntentService, OpenAiCompatibleGroqChatClient,
│                   IntentExtractionPrompt
├── payment         RazorpayPaymentService, SdkRazorpayOrderClient
├── entity          JPA entities: User, PurchasePolicy, Transaction, AuditEvent
├── repository      Spring Data repositories with custom JPQL queries
├── dto             Request/response records
├── exception       GlobalExceptionHandler + typed exceptions
└── config          GroqClientConfig, RazorpayClientConfig, CorsConfig
```

---

## API Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/users` | Create a user |
| `GET` | `/api/users/{id}` | Get a user by ID |
| `POST` | `/api/policies` | Create a purchase policy for a user |
| `GET` | `/api/policies/user/{userId}` | Get all policies for a user |
| `POST` | `/api/intent/parse` | Parse natural-language request into structured intent (Groq only) |
| `POST` | `/api/agent/evaluate` | Parse intent then evaluate against policy engine (full flow) |
| `POST` | `/api/transactions/evaluate` | Evaluate directly with explicit category (no LLM) |
| `GET` | `/api/transactions/user/{userId}` | Get transaction history for a user |
| `GET` | `/api/audit/transaction/{transactionId}` | Get audit events for a transaction |
| `POST` | `/api/payments/create` | Create a Razorpay payment order for an approved transaction |

All error responses follow a consistent `ApiError` shape: `timestamp`, `status`, `error`, `message`, `path`, and optional `fields` for validation errors.

---

## Testing

The test suite covers the deterministic policy engine (19 cases), Razorpay payment service, Groq intent service, agent evaluation service, user/policy services, and all controllers using MockMvc.

```
Tests run: 47, Failures: 0, Errors: 0, Skipped: 0
```

---

## Limitations / Known Issues

**Concurrency race condition:** The hourly spending total is read and the transaction is persisted in a single database transaction, but no per-user advisory lock or serializable isolation is used. Two concurrent evaluations for the same user can both observe the same prior hourly total, potentially allowing both to pass a limit that only one should pass. A production deployment should add database-level per-user locking or an atomic spending ledger before making race-safety claims.

**No authentication:** The API has no authentication layer. Any caller can submit requests for any `userId`. This is intentional for the buildathon demo scope.

**LLM non-determinism:** Groq intent extraction produces structured output under a strict schema, but the `category` string is free-form and must match a user's policy category exactly (case-insensitive). Users should configure policy categories that correspond to what the LLM naturally returns for their purchase types.

---

## Repository Structure

```
agentguard/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/agentguard/
│   │   │   ├── AgentGuardApplication.java
│   │   │   ├── config/          GroqClientConfig, RazorpayClientConfig, CorsConfig
│   │   │   ├── controller/      REST controllers
│   │   │   ├── dto/             Request/response records
│   │   │   ├── entity/          JPA entities + enums
│   │   │   ├── exception/       GlobalExceptionHandler + exception types
│   │   │   ├── intent/          Groq integration, intent parsing
│   │   │   ├── payment/         Razorpay integration
│   │   │   ├── policy/          DeterministicPolicyEngine
│   │   │   ├── repository/      Spring Data repositories
│   │   │   └── service/         Business logic
│   │   └── resources/
│   │       └── application.yml
│   └── test/
│       └── java/com/agentguard/
│           ├── controller/      MockMvc controller tests
│           ├── entity/          Entity tests
│           ├── intent/          Groq intent service tests
│           ├── payment/         Razorpay payment service tests
│           ├── policy/          DeterministicPolicyEngine tests (19 cases)
│           └── service/         Service unit tests
└── frontend/
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.jsx
        ├── App.jsx             Dashboard, policy management, evaluation UI
        ├── App.css
        ├── api.js              API client
        └── index.css
```
