# Cloth Shop — E-Commerce Platform

A clothing storefront built on a Spring Cloud microservices backend (9 independently deployable services) with a Next.js frontend. Checkout is an asynchronous saga over Kafka: an order is created, stock is reserved, payment is charged via Stripe, and every failure path compensates cleanly (cancel the order, release reserved stock) instead of leaving things half-done.

For the full architecture, the checkout saga's message flow, and known gotchas, see [CLAUDE.md](CLAUDE.md). For the production roadmap, what's shipped vs. deliberately deferred, and why, see [PLAN.md](PLAN.md).

## Architecture at a glance

| Service | Role | Store |
|---|---|---|
| `discovery-service` | Eureka registry | — |
| `config-server` | Centralized config (git-backed) | — |
| `api-gateway` | Entry point: routing, OAuth2/JWT, CORS, rate limiting, circuit breaking | Redis (rate limiter) |
| `customer-service` | Customers + addresses | MongoDB |
| `product-service` | Catalog: products, size/color variants, stock | Postgres |
| `cart-service` | Shopping cart | Redis |
| `order-service` | Order orchestration, checkout saga | Postgres |
| `payment-service` | Payment records, Stripe charges | Postgres |
| `notification-service` | Order/payment confirmation emails | MongoDB |
| `frontend` | Next.js storefront (the only client of `api-gateway`) | — |

Identity is Keycloak (OAuth2/OIDC); tracing is Zipkin; local infra (Postgres, MongoDB, Kafka, Redis, Keycloak, MailDev, Zipkin) runs via `docker-compose.yml`.

## Prerequisites

See [requirements.txt](requirements.txt) for exact tool versions. In short: JDK 21, Maven, Node.js 20+, Docker & Docker Compose.

## Quick start

```bash
# 1. Start local infrastructure (Postgres, MongoDB, Kafka, Redis, Keycloak, MailDev, Zipkin)
docker compose up -d

# 2. Provision the Next.js OAuth2 client in Keycloak (one-time, needs Keycloak up first)
./keycloak/setup-nextjs-client.sh

# 3. Start the backend services, each in its own terminal, from services/<name>/
#    Order matters: config-server and discovery-service first, then the rest.
`cd services/config-server && ./mvnw spring-boot:run
cd services/discovery-service && ./mvnw spring-boot:run
cd services/api-gateway && ./mvnw spring-boot:run
cd services/customer-service && ./mvnw spring-boot:run
cd services/product-service && ./mvnw spring-boot:run
cd services/cart-service && ./mvnw spring-boot:run
cd services/order-service && ./mvnw spring-boot:run
cd services/payment-service && ./mvnw spring-boot:run
cd services/notification-service && ./mvnw spring-boot:run`

# 4. Start the frontend
cd frontend
cp .env.local.example .env.local   # fill in AUTH_SECRET (npx auth secret), etc.
npm install
`npm run dev`
```

The frontend runs at `http://localhost:3000`, api-gateway at `http://localhost:8222`.

## Repository layout

```
services/           9 independent Maven projects (one Spring Boot app each, no parent POM)
frontend/           Next.js 16 app (App Router, TypeScript, Tailwind)
keycloak/           Keycloak Admin API scripts (client provisioning)
docker-compose.yml  Local dev infrastructure
CLAUDE.md           Architecture reference and AI-assistant guide
PLAN.md             Production roadmap: phases shipped, known simplifications, open decisions
```

## Testing

- Backend: `mvn test` from inside each `services/<name>/` directory.
- Frontend: `npm run lint` and `npx tsc --noEmit` from inside `frontend/`.

## Status

Core storefront flow (browse → variant selection → cart → checkout → async payment) is implemented end to end, backend and frontend. See [PLAN.md](PLAN.md) for what's deliberately simplified (e.g. no consumer-side Kafka dedup, order history isn't customer-scoped yet) versus what's still planned (Phase 6: caching, observability, integration tests, CI/CD, secrets hardening).
