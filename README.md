# AgentGuard — The Financial Firewall for AI Agents

AgentGuard is a backend boundary between an AI purchasing agent and payment systems. Its architecture is deliberately separated: an LLM may interpret user intent, but a deterministic policy engine decides, and the payment service executes only approved decisions. The LLM never approves, blocks, or executes a payment.

## Current scope

The backend provides user and purchase-policy management, deterministic transaction evaluation, read endpoints for transactions and audit events, MySQL persistence, request validation, and consistent error responses. Payment-provider integration and all LLM work are deferred.

## Why the Policy Engine is deterministic

The LLM is intentionally isolated from financial authority. The future LLM layer will translate natural-language user intent into structured `PurchaseIntent` data. The deterministic `PolicyEngine` independently validates that intent against enforceable financial policies. Only the PolicyEngine's approved decision can reach the payment layer.

```text
Future LLM (intent interpretation only)
              |
              v
       PurchaseIntent (untrusted)
              |
              v
PolicyEvaluationService -> DeterministicPolicyEngine -> Decision + audit record
                                                        |
                                                        v
                                            Future payment layer (approved only)
```

The current risk score is a transparent, rule-based heuristic—not an ML fraud score. It adds risk for amounts near the user limit, projected hourly spending near the hourly limit, and recent duplicates. It is designed to be replaceable by a future risk model.

## AI Intent Layer

AgentGuard uses an LLM only as an intent interpreter. Groq converts a natural-language purchase request into a constrained `PurchaseIntent`; financial authority remains with the deterministic `PolicyEngine`. The parsed intent is validated server-side before it is accepted, and it can only supply category, preferred brand, maximum amount, and category-match requirements.

## Security Boundary

```text
LLM
 |
 v
PurchaseIntent
 |
 v
PolicyEngine
 |
 v
Decision
 |
 v
Future payment layer
```

The LLM cannot change spending limits, modify policies, bypass the policy engine, approve payments, or call payment APIs. `/api/agent/evaluate` parses intent and then always invokes the deterministic policy flow. It also checks the actual transaction amount independently against the intent maximum.

### Groq configuration

Set `GROQ_API_KEY` in the environment; it is never committed or logged. `GROQ_MODEL` defaults to `openai/gpt-oss-120b` and `GROQ_BASE_URL` defaults to `https://api.groq.com/openai/v1`. The HTTP client uses a 5-second connection timeout and a 15-second response timeout. A missing, malformed, or unavailable provider response fails safely and creates no transaction.

### Concurrency limitation

The hourly total is read and the transaction is then stored in one database transaction, but Phase 2 does not acquire a per-user lock or use serializable database isolation. Two concurrent evaluations can therefore both observe the same prior hourly total. A future phase should introduce database-level per-user locking or an atomic spending ledger before making race-safety claims.

## Configure MySQL

Copy `.env.example` into your preferred environment configuration and set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. The configured MySQL user needs permission to create and update the `agentguard` schema tables.

## Run

Use Java 21 and Maven:

```powershell
$env:DB_URL='jdbc:mysql://localhost:3306/agentguard?serverTimezone=UTC'
$env:DB_USERNAME='agentguard_user'
$env:DB_PASSWORD='your_password'
mvn spring-boot:run
```

## Test

```powershell
mvn test
```

The current APIs are `POST /api/users`, `GET /api/users/{id}`, `POST /api/policies`, `GET /api/policies/user/{userId}`, `POST /api/intent/parse`, `POST /api/agent/evaluate`, `POST /api/transactions/evaluate`, `GET /api/transactions/user/{userId}`, and `GET /api/audit/transaction/{transactionId}`.
