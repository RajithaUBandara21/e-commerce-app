# Production Plan — Cloth Shop Platform

Status: draft roadmap. This is the target state and the path to it, not the current state — see [CLAUDE.md](CLAUDE.md) for what exists today. Chosen frontend integration is **Option A: Next.js calls `api-gateway` directly** (see prior architecture comparison).

## 1. Current state (baseline)

Eight independent Maven services, Java 21 / Spring Boot 3.5.13 / Spring Cloud 2025.0.2, no aggregator POM:

| Service | Store | Role today |
|---|---|---|
| `discovery-service` | — | Eureka registry |
| `config-server` | git (`e-com-app-config`) | centralized config, port 8888 |
| `api-gateway` | — | routing + OAuth2 resource server (Keycloak, realm `micro`) |
| `customer-service` | MongoDB | customer + address documents |
| `product-service` | Postgres | catalog; **now variant-aware** (`Product` → `ProductVariant`: sku/size/color/stock) |
| `order-service` | Postgres | order + order-line orchestration, calls customer/product/payment |
| `payment-service` | Postgres | payment records, publishes `payment-topic` |
| `notification-service` | MongoDB | consumes `order-topic`/`payment-topic`, sends email via MailDev |

Known gaps that block "production grade," found while reading the code (not hypothetical):

- **No order status.** `Order` (`services/order-service/.../entity/Order.java`) has no state field at all — a created order and a paid order look identical in the database.
- **No saga / compensation.** `OrderServiceImpl.createOrder` decrements stock via `productClient.purchaseProducts(...)`, *then* calls `paymentClient.requestOrderPayment(...)` synchronously. If payment fails or the call throws, stock stays decremented — an oversold or stuck order with no rollback path.
- **No idempotency.** `createOrder` has no dedup key; a retried request (network blip, double-click) creates a second order and decrements stock twice.
- **Stale cross-service contract.** `product-service`'s purchase contract now takes `variantId` (this session's change); `order-service`'s `PurchaseRequestDTO`/`OrderLine`/`ProductClient` still send `productId`. Needs updating before checkout works end-to-end.
- **No optimistic locking on stock.** `ProductVariant.availableQuantity` is read-modify-written with no `@Version` — two concurrent purchases can both read the same quantity and both succeed (oversell).
- **`api-gateway`/`order-service` mix client styles.** `CustomerClient`/`PaymentClient` are Feign; `ProductClient` is a raw `RestTemplate` (see [CLAUDE.md](CLAUDE.md) gotcha). No retry, timeout, or circuit breaker on any of them.
- **Hardcoded secret.** `services/config-server/src/main/resources/application.yml` has a live GitHub PAT committed to git (flagged separately — revoke and move to an env var before anything else ships).
- **No cart.** There is no cart entity or endpoint anywhere; checkout goes straight from a purchase-request list to an order.
- **No product images.** `Product` has no image/media field.
- **No rate limiting, no caching layer, no search index.**

Everything below is scoped against these specific gaps, not a generic checklist.

## 2. Target architecture

```
Next.js (App Router)              →  api-gateway  →  discovery-service (Eureka)
  Server Components: catalog/PDP        (Keycloak JWT,        config-server (git-backed config)
  Client Components: cart/checkout       CORS, rate limit)
  NextAuth (Keycloak provider, PKCE)          │
                                    ┌─────────┼──────────┬─────────────┬───────────────┐
                              customer-svc  product-svc  order-svc   payment-svc   notification-svc
                               (MongoDB)     (Postgres)  (Postgres)   (Postgres)     (MongoDB)
                                                  │            │
                                             cart-service   Kafka: order-topic, payment-topic,
                                             (new, Redis)   stock-release-topic (new, compensation)
```

`api-gateway` stays the single entry point for the browser (Option A). Redis is introduced for cart + gateway rate limiting + product-read caching. Kafka gains one new topic for saga compensation.

## 3. Data structures & algorithms — key decisions

The instinct to "just make it work" tends to pick the data structure that's easiest to write, not the one that's correct under concurrency or scale. Concrete calls for this codebase:

| Concern | Wrong instinct | Correct choice here | Why |
|---|---|---|---|
| Stock decrement (`ProductVariant.availableQuantity`) | Read quantity, subtract in Java, write back (current code) | Add `@Version` (JPA optimistic locking) **or** a single atomic `UPDATE product_variant SET available_quantity = available_quantity - :qty WHERE id = :id AND available_quantity >= :qty` | The current read-then-write has a lost-update race: two concurrent purchases can both read stock=1 and both succeed. An atomic conditional update is one round trip and can't oversell. |
| Order state | Booleans (`isPaid`, `isShipped`, `isCancelled`) | Single `OrderStatus` enum (`PENDING_PAYMENT`, `CONFIRMED`, `PAYMENT_FAILED`, `CANCELLED`, `SHIPPED`, `DELIVERED`, `REFUNDED`) with explicit allowed transitions | Booleans allow impossible states (`isPaid=true` and `isCancelled=true` at once). A state machine makes invalid combinations unrepresentable. |
| Catalog pagination | `OFFSET`/`LIMIT` | Keyset (seek) pagination: `WHERE id > :cursor ORDER BY id LIMIT :n` | `OFFSET` cost grows linearly with page depth (DB still scans and discards N rows). Keyset pagination is a B-tree index lookup regardless of how deep the page is. |
| Cart storage | A `cart_items` JSON blob column rewritten on every add/remove | Redis hash `cart:{userId}` → field `variantId`, value `qty`, with a TTL | O(1) per-item add/update/remove instead of read-modify-write the whole blob; TTL gives free abandoned-cart expiry without a cron job. |
| Retried checkout requests | No dedup — a retry creates a second order | Client sends an `Idempotency-Key` header; server enforces a unique index (Postgres) or `SETNX` (Redis) on that key before creating the order | Turns "did this already happen?" into an O(1) index/key lookup instead of trusting the client not to double-submit. |
| Filtering by category/size/color/price | Full scan / `LIKE` on unindexed columns | Composite B-tree indexes on `(category_id, size, color)`; range index on `price` | Structured, low-cardinality filters (size, color, category) are exactly what a B-tree composite index is for — no need to reach for Elasticsearch until free-text search is an actual requirement. |
| Category hierarchy (if "Men → Shirts → Casual" is needed) | Nested-set model | Adjacency list: self-referencing `parent_id` on `Category` | Nested-set makes writes (moving a category) expensive to buy cheap subtree reads you don't need at this catalog depth. Adjacency list + a recursive CTE for the rare full-tree read is simpler and correct for a few hundred categories. |
| Gateway rate limiting | In-memory counter per gateway instance | Redis-backed token bucket (Spring Cloud Gateway `RequestRateLimiter` + `spring-boot-starter-data-redis-reactive`) | An in-memory counter is per-instance and resets on restart — it stops being a real limit the moment the gateway is scaled past one replica. |

## 4. Phased roadmap

### Phase 0 — done this session
- `product-service`: `ProductVariant` (sku/size/color/stock), purchase flow keyed on `variantId`. Tests updated and passing.

### Phase 1 — close the correctness gaps (before any new feature work)
1. Add `@Version` to `ProductVariant` (optimistic locking) — smallest fix for the oversell race.
2. Add `OrderStatus` to `Order` + migration; wire it through `createOrder`/`getOderById`.
3. Update `order-service`'s `PurchaseRequestDTO`/`OrderLine`/`ProductClient` to send `variantId`, matching `product-service`'s new contract.
4. Add `Idempotency-Key` handling to `POST /api/v1/orders` (or wherever order creation ends up).
5. Rotate the leaked GitHub PAT in `config-server`'s `application.yml`; replace with `${GITHUB_TOKEN}`.

### Phase 2 — checkout consistency (saga)
Replace the synchronous "decrement stock → call payment inline" chain with choreography over Kafka:
1. `order-service` creates order as `PENDING_PAYMENT`, publishes `OrderCreated`.
2. `product-service` reserves/decrements stock on `OrderCreated`, publishes `StockReserved` or `StockReservationFailed`.
3. `payment-service` charges on `StockReserved`, publishes `PaymentCompleted`/`PaymentFailed` (topic already exists: `payment-topic`).
4. `order-service` consumes payment result → `CONFIRMED` or `CANCELLED`; on `StockReservationFailed` or `PaymentFailed`, publish `StockReleaseRequested` (new topic) so `product-service` restores quantity.
5. `notification-service` keeps consuming `order-topic`/`payment-topic` as it does today — no change needed there.

This removes the current failure mode (stock decremented, payment never confirmed, no way back).

### Phase 3 — cart
- New `cart-service` (Redis-backed, per the data-structure table above) **or** extend `order-service` with a `Cart`/`CartItem` concept that gets converted to an `Order` at checkout. Decide based on Phase 4 frontend needs — a cart that's just "add/remove/view" fits a small Redis-backed service better than another Postgres schema.
- Endpoints: `POST /api/v1/cart/items`, `DELETE /api/v1/cart/items/{variantId}`, `GET /api/v1/cart`, `POST /api/v1/cart/checkout` (hands off to order creation).

### Phase 4 — auth + gateway hardening
- Keycloak realm `micro`: add a public client for Next.js (PKCE, redirect URIs for `localhost:3000` and prod domain), and confirm role mapping (customer vs admin) reaches the JWT.
- `api-gateway`: CORS for the Next.js origin; Redis-backed `RequestRateLimiter`; Resilience4j circuit breaker + retry + timeout wrapping the downstream routes (or push this to each service's Feign clients — pick one place, not both).
- Unify `order-service`'s HTTP clients on Feign (retire the raw `RestTemplate` `ProductClient`) so resilience config is consistent across `CustomerClient`/`PaymentClient`/`ProductClient`.

### Phase 5 — Next.js frontend
- App Router: `/` (catalog), `/products/[slug]` (PDP with size/color variant picker sourced from `ProductResponseDTO.variants`), `/cart`, `/checkout`, `/account/orders`.
- Server Components fetch catalog/PDP data straight from `api-gateway` (SSR/ISR with revalidation on stock changes); Client Components own cart/variant-selection interactivity.
- NextAuth.js with the Keycloak provider from Phase 4; session holds the access token for calling the gateway.
- Client-side data layer: TanStack Query for request caching/retries against the gateway; keep cart mutations optimistic against the Phase 3 cart endpoints.
- Product images: since `Product` has no image field yet, add an `imageUrl`/`gallery` field backed by object storage (S3-compatible; MinIO locally via `docker-compose.yml`) rather than storing blobs in Postgres.

### Phase 6 — production hardening
- **Caching**: Redis cache-aside in front of `GET /api/v1/products` and `GET /api/v1/products/{id}` (read-heavy, changes only on stock/price updates — invalidate on write).
- **Search**: start with the composite Postgres indexes from §3; only add Elasticsearch if free-text search becomes a real requirement, not preemptively.
- **Observability**: keep Zipkin (already wired via `micrometer-tracing-bridge-brave`); add Prometheus scraping off each service's `/actuator/prometheus` and a Grafana dashboard for request latency, Kafka consumer lag, and stock-reservation failure rate.
- **Testing**: Testcontainers-backed integration tests per service (Postgres/Mongo/Kafka), replacing the current pure-Mockito unit tests for the saga-critical paths (`OrderServiceImplTest`, the new stock-reservation consumer); Playwright e2e against a full docker-compose stack for the Next.js checkout flow.
- **CI/CD**: extend the existing per-service Jenkinsfiles (`buildService(...)` shared-lib calls) with a test gate before the Docker build step; add a Next.js pipeline (lint, type-check, build, Playwright smoke test).
- **Secrets**: everything currently inline in `application.yml` (Keycloak admin creds, Postgres/Mongo creds in `docker-compose.yml`, the leaked PAT) moves to environment variables or a secrets manager before any non-local deployment.

## 5. Decisions (locked 2026-08-15)

1. **Cart** — new `cart-service`, Redis-backed (§3 design: `cart:{userId}` hash).
2. **Payment provider** — real Stripe integration in `payment-service` (Phase 2 saga charges via Stripe, not a mock).
3. **Hosting target** — Docker Compose / single VM for production, matching the current dev setup. Phase 6 tooling (secrets, observability) is scoped to that, not Kubernetes.
4. **Category hierarchy** — flat `Category` for v1, no `parent_id`. Revisit only if the catalog actually grows multi-level.
