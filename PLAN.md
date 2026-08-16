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

### Phase 1 — close the correctness gaps (before any new feature work) — done 2026-08-15
1. ✅ `@Version` added to `ProductVariant`; concurrent-save conflicts now surface as a clear `ProductPurchaseException` ("changed concurrently, please retry") instead of an oversell or a raw 500.
2. ✅ `OrderStatus` (`PENDING_PAYMENT`/`CONFIRMED`/`PAYMENT_FAILED`/`CANCELLED`/`SHIPPED`/`DELIVERED`/`REFUNDED`) added to `Order`; `createOrder` now sets it at each transition and a payment-client failure is caught, persisted as `PAYMENT_FAILED`, and rethrown instead of silently leaving stock decremented with no record.
3. ✅ `order-service`'s `PurchaseRequestDTO`/`PurchaseResponseDTO`/`OrderLine`/`OrderLineRequestDTO` migrated from `productId` to `variantId`, matching `product-service`'s contract. `notification-service`'s copy of `PurchaseResponseDTO` updated to match (same Kafka payload shape, now carrying `variantId`/`size`/`color`).
4. ✅ `Idempotency-Key` header on `POST /api/v1/orders`: a repeated key returns the existing order id and skips customer/stock/payment calls entirely (checked via `OrderRepository.findByIdempotencyKey`, unique-constrained column).
5. ✅ Leaked GitHub PAT in `config-server`'s `application.yml` replaced with `${GITHUB_TOKEN}`. **Still needs manual action**: revoke the old token at github.com/settings/tokens and scrub it from git history — this edit only stops it from being read going forward, it doesn't remove it from past commits.

All changes covered by unit tests (product-service: 11/11 passing; order-service: 6/6 passing; notification-service compiles and context-loads clean).

### Phase 2 — checkout consistency (saga) — done 2026-08-15
Replaced the synchronous "decrement stock → call payment inline" chain with Kafka choreography, correlated by `orderReference`. See [CLAUDE.md](CLAUDE.md)'s "Checkout saga" section for the full topic-by-topic flow. Summary of what shipped:

1. ✅ `order-service` creates the order as `PENDING_PAYMENT` and publishes `OrderCreatedEventDTO` to `order-created-topic` — no more direct HTTP calls to product-service/payment-service (`ProductClient`, `PaymentClient`, `RestTemplateConfig` deleted, along with order-service's now-dead `PaymentRequestDTO`).
2. ✅ `product-service`'s `OrderCreatedConsumer` reserves stock (reusing `purchaseProductService`, so optimistic-locking/insufficient-stock handling carries over unchanged) and publishes success/failure to `stock-topic`.
3. ✅ `order-service`'s `StockReservationConsumer` reacts to `stock-topic`: sends the order-confirmation email on success, cancels the order on failure.
4. ✅ `payment-service`'s own `stock-topic` consumer charges via **Stripe** (new: `StripePaymentService`, `com.stripe:stripe-java`) on success only, publishes outcome to `payment-topic` (`PaymentNotificationRequestDTO`/`PaymentConfirmationDTO` gained `success`/`reason` fields).
5. ✅ `order-service`'s `PaymentResultConsumer` reacts to `payment-topic`: `CONFIRMED` on success; `PAYMENT_FAILED` + publishes `StockReleaseEventDTO` to `stock-release-topic` on failure.
6. ✅ `product-service`'s `StockReleaseConsumer` restores quantity via the new `ProductService.releaseStock`.
7. ✅ `notification-service`'s payment listener now guards on `success` — a failed payment is logged, not emailed (no failure email template built yet, by design — see Known simplifications below).

This removes the original failure mode (stock decremented, payment never confirmed, no way back) — a failed reservation cancels cleanly with nothing to release; a failed payment cancels **and** releases the reservation.

**Known simplifications, not oversights:**
- No consumer-side dedup for redelivered Kafka messages (at-least-once delivery could in theory double-reserve/double-charge on redelivery). Worth revisiting in Phase 6 if this goes to real production load.
- Stripe charging requires a `stripePaymentMethodId` sourced from a frontend using Stripe Elements/Stripe.js — there is no frontend yet (Phase 5), so real charges can't happen end-to-end until then. Without one, the charge fails closed with a clear reason ("No Stripe payment method provided") rather than faking success.
- `payment-service`'s original `POST /api/v1/payments` (`PaymentController`/`createPayment`) is untouched and still bypasses Stripe entirely (always "succeeds") — it's a separate, non-saga entry point, not a bug.

### Phase 3 — cart — done 2026-08-15
New `cart-service` (9th service), Redis-backed per the decision in §5. `GET/DELETE /api/v1/cart/{userId}`, `POST /api/v1/cart/{userId}/items` (increment), `PUT /api/v1/cart/{userId}/items/{variantId}` (set exact quantity, `0` removes), `POST /api/v1/cart/{userId}/checkout` (builds an `OrderRequestDTO` from the cart's items and calls `order-service` via `OrderClient` Feign, honoring the checkout request's own `Idempotency-Key` pass-through). `CartRepository` wraps `StringRedisTemplate` hash ops directly on `cart:{userId}` — no Spring Data Redis repository abstraction, since that doesn't fit the "one hash per cart" shape. `redis:7-alpine` added to `docker-compose.yml` (port 6379). 7/7 tests passing.

**Known limitation, inherited from order-service, not new**: `CartCheckoutRequestDTO.totalAmount` is client-supplied, not recomputed server-side from `product-service`'s authoritative variant prices — `order-service`'s `OrderRequestDTO.totalAmount` already had this gap before cart-service existed. Worth a dedicated fix across both services together, not a cart-service-only patch.

### Phase 4 — auth + gateway hardening — done 2026-08-15
- ✅ *Already resolved as a side effect of Phase 2*: `order-service`'s HTTP clients are unified — `ProductClient`/`PaymentClient` were deleted when the saga replaced their synchronous calls, leaving only `CustomerClient` (Feign). Nothing left to "unify."
- ✅ `api-gateway` CORS: `SecurityConfiguration` now has a `CorsConfigurationSource` allowing `${application.config.frontend-origin:http://localhost:3000}` with credentials, wired into the security filter chain. (Also fixed a pre-existing one-character typo — `"(/eureka/**"` — that silently broke the Eureka path exemption.)
- ✅ Redis-backed `RequestRateLimiter` (20 req/s, burst 40, per-remote-address via a new `KeyResolver` bean) applied gateway-wide via `spring.cloud.gateway.default-filters` — this works regardless of how routes are defined, since routing config for this gateway lives in the external `e-com-app-config` repo, not here.
- ✅ Resilience4j `CircuitBreaker` (`defaultCircuitBreaker`, 50% failure threshold trips it, falls back to `/fallback`) + Gateway's built-in `Retry` (3 attempts, GET only — deliberately excludes POST/PUT so a retry can't double-submit a non-idempotent request) + `httpclient` connect/response timeouts, all gateway-wide via the same `default-filters` mechanism.
- ✅ `keycloak/setup-nextjs-client.sh` — a runnable (not just documented) Keycloak Admin REST API script that provisions a public `nextjs-storefront` client (authorization code + PKCE, no secret) in the `micro` realm. **You need to actually run this** against your running Keycloak (`docker compose up -d keycloak` first) — nothing about the `micro` realm lives in this repo (it was hand-created in the admin console originally), so this is the closest to "in-repo" that client provisioning can get without a full realm-export.

### Phase 5 — Next.js frontend — done 2026-08-15
New `frontend/` app: Next.js 16 (App Router, Turbopack), TypeScript, Tailwind. Built against the **real** Next.js 16 docs bundled in `node_modules/next/dist/docs/` (not training-data assumptions) — this version renamed `middleware.ts` → `proxy.ts` and changed default `fetch` caching (no longer cached by default), both of which shaped the code below.

- ✅ Pages: `/` (catalog, Server Component), `/products/[id]` (PDP with a client-side size/color `VariantPicker`), `/cart` (Server Component enrichment + `CartView` client component for quantity/remove/checkout), `/checkout/[orderId]` (order status after checkout).
- ✅ `src/auth.ts` — NextAuth v5 (`next-auth@beta`) with the Keycloak provider (PKCE, `token_endpoint_auth_method: none`, matching the public client from Phase 4's setup script) and an `authorized` callback so `src/proxy.ts` actually gates `/cart`, `/checkout`, `/account`.
- ✅ Cart/checkout mutations go through Next.js **Server Actions** (`src/lib/actions.ts`), not client-side fetch — they call `auth()` for the session server-side, then `src/lib/api.ts`'s typed gateway client.
- ✅ **Gateway fix required to unblock this phase**: `api-gateway`'s `SecurityConfiguration` required a JWT on *every* request, which would have made anonymous catalog browsing impossible. Added `permitAll()` for `GET /api/v1/products/**` (also fixed a pre-existing one-character typo — `"(/eureka/**"` — found while in the same method). Assumes the gateway preserves each service's own `/api/v1/<resource>` path; verify against the actual route config in `e-com-app-config` if routes turn out to be prefixed differently.
- ✅ `keycloak/setup-nextjs-client.sh` (Phase 4) is what this phase's `AUTH_KEYCLOAK_ID`/issuer point at — see `.env.local.example`.

**A real bug hit and fixed while building this, worth remembering**: TypeScript module augmentation (`declare module "next-auth" { ... }`) silently breaks — replacing the real module's types instead of merging with them — if the `.d.ts` file doing the augmenting has no top-level `import`/`export` of its own (it becomes an ambient script, not a module). `src/types/next-auth.d.ts` has an `export {}` specifically to prevent this; removing it reintroduces a `NextAuth(...)` "not callable" error with no indication the cause is the augmentation file. Also: `@auth/core`'s callback signatures import `JWT` from `@auth/core/jwt` internally, not from `next-auth/jwt` — augmenting only the latter leaves `token.accessToken` typed `unknown` inside `callbacks.session`/`callbacks.jwt`. Both modules need the same augmentation.

**Known simplifications, not oversights:**
- `session.accessToken` is exposed to client-side `useSession()` (NextAuth v5 shares one session shape between `auth()` and `useSession()`) — hardening this to a server-only token via `next-auth/jwt`'s `getToken()` is real but deferred work, noted in `src/auth.ts`.
- The cart page enriches `cart-service`'s bare `{variantId, quantity}` items by fetching the *entire* product catalog and matching client-side — there's no `GET /api/v1/products/variants/{id}` lookup endpoint yet. Fine at small catalog scale; add a dedicated endpoint if that stops being true.
- No "my orders" page: `order-service`'s `GET /api/v1/orders` returns every order unfiltered, not scoped to the caller — deliberately not building a page on top of a data leak. Needs customer-scoping in `order-service` first.
- No custom-branded sign-in page — uses NextAuth's default provider-list page.
- Product images: `Product` still has no image field; the PDP renders a placeholder block. Needs an `imageUrl`/gallery field backed by object storage (S3-compatible; MinIO locally), per the original Phase 5 plan — not done.
- `CartCheckoutRequestDTO.totalAmount` is computed client-side from catalog prices already fetched into the page, same trust model as `OrderRequestDTO.totalAmount` (see Phase 3's note) — not server-recomputed.

### Phase 6 — Seller identity & marketplace foundation — done 2026-08-15

The project pivoted from "single-seller storefront, production-hardening only" to a genuine **multi-vendor marketplace**, per a broader follow-up request. This phase (and 7–16 below) supersede the single old "Phase 6 — production hardening" outline; see the "Cloth Shop → Production-Grade Multi-Vendor Marketplace" plan for the full architecture rationale (decisions locked in §5 below).

- ✅ New **`seller-service`** (10th service, Postgres): `Seller` entity (`keycloakUserId`, business profile, `SellerStatus` PENDING/ACTIVE/SUSPENDED, Stripe Connect fields reserved for Phase 7). `POST /api/v1/sellers/register`, `GET /api/v1/sellers/me`, `GET /api/v1/sellers` (admin), `PATCH /api/v1/sellers/{id}/status` (admin) — approving a seller (`status → ACTIVE`) calls Keycloak's Admin REST API to grant the `seller` realm role; suspending revokes it. 6/6 tests passing.
- ✅ `keycloak/setup-realm-roles.sh` (creates `customer`/`seller`/`admin` realm roles, idempotent) and `keycloak/setup-seller-service-client.sh` (provisions seller-service's confidential service-account client, grants it `manage-users`/`view-users`/`view-realm` on `realm-management` — all three needed: the first two to look up a user and write role mappings, the third because *reading* a realm role's representation is gated separately and 403s without it even with manage-users alone).
- ✅ `product-service`: `Product.sellerId` (Keycloak subject, string — same non-FK convention as `Order.customerId`). `POST/PUT/DELETE /api/v1/products` now ownership-checked (`ProductAccessDeniedException` → 403 unless caller's id matches `sellerId` or caller is admin); `GET /api/v1/products?sellerId=` for "my catalog". `PUT`/`DELETE /api/v1/products/{id}` didn't exist before this phase — sellers had no way to edit or remove their own listings at all, a real gap now closed.
- ✅ Full **Category CRUD** in product-service (previously flagged as a gap — only the JPA entity existed, no controller/service/repository at all): public reads, admin-only writes (`X-User-Roles` header check). Deleting a non-empty category is rejected (`CategoryNotEmptyException`, 409) rather than silently cascading — `Category.products` has `cascade = CascadeType.REMOVE`, which would otherwise delete every product in the category as a side effect of an admin action.
- ✅ `api-gateway`: `SecurityConfiguration` gained a `JwtAuthenticationConverter` unpacking Keycloak's `realm_access.roles` claim into `ROLE_*` authorities (previously unused — no role-based authorization existed anywhere in the gateway) and role-gated path matchers (`ROLE_ADMIN` for category mutations and seller admin endpoints, `ROLE_SELLER`/`ROLE_ADMIN` for product mutations). New `UserContextGatewayFilter` (a `GlobalFilter`) forwards `X-User-Id`/`X-User-Roles` downstream from the validated JWT, stripping any inbound values first so a client can't spoof them — this is what lets product-service/seller-service authorize without each re-validating a JWT themselves (the gateway is the trust boundary, documented in `SecurityConfiguration`'s class javadoc).
- ✅ Verified against the real, running local stack (not just unit tests): seller registers → admin approves → Keycloak role granted (confirmed by decoding a freshly-issued token) → seller creates/updates/deletes a product with the correct `sellerId` → category admin-only enforcement holds.

**Real bugs hit and fixed while verifying this end-to-end (worth remembering):**
- **`exchange.getPrincipal()` is not reliably populated inside a Spring Cloud Gateway `GlobalFilter`**, even though the request is fully authenticated by the time the filter runs — it read as empty every time. `ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication)` is the mechanism that's actually guaranteed to carry the principal through (backed by the Reactor `Context` Spring Security's `AuthenticationWebFilter` writes into via `.contextWrite(...)`), and is what `UserContextGatewayFilter` uses now.
- **`RestClient`'s `"{var}"` URI-template substitution percent-encodes whatever you pass in** — `KeycloakRoleClient` originally built URLs as `.uri("{baseUrl}/admin/realms/{realm}/...", properties.baseUrl(), ...)`, and since the template variable's *value* (a full `http://host:port` string) isn't recognized as a scheme+authority by `UriComponentsBuilder`, its `:`/`/` (and, with an IPv6 literal host, even `[`/`]`) get encoded, breaking the URL. Fixed by building the full URL as a plain concatenated string and passing it via `.uri(URI.create(...))`, which parses it as a literal URI with no re-encoding. This bug existed for *any* `baseUrl` value, not just the IPv6 one below — it just hadn't been exercised yet.
- **Local-environment-only**: an unrelated Apache httpd on the dev machine owns `127.0.0.1:8080`, so Docker's Keycloak port-forward only bound IPv6 (`[::1]:8080`) — `localhost:8080` resolved inconsistently depending on the resolver (curl happened to prefer IPv6; the JVM's `WebClient`/`RestClient` didn't). Worked around entirely in local scratch config (`issuer-uri`/`keycloak.admin.base-url` pointed at the `[::1]` literal) — not a code or repo-config change, since real deployments won't have this conflict.
- **Keycloak's `admin-cli` client issues near-empty access tokens** (no `sub`, no `realm_access` — just `exp`/`iat`/`azp`/`scope`) when used for manual password-grant testing; a client with the default full client-scope set (like `nextjs-storefront`) is needed to get a token with the claims application code actually depends on. Not a bug, but a real trap when hand-testing with `curl` — worth remembering for future manual verification.

**Known simplifications, not oversights:**
- No synchronous coupling from product-service to seller-service (product mutation doesn't check the seller's `ACTIVE`/Stripe-onboarding status at write time) — enforcement is via the Keycloak `seller` role itself, granted only on admin approval, so this is enforced at the auth layer, not via an extra network call on every write.
- Gateway routing for `seller-service` (`/api/v1/sellers/**`) is only defined in this session's local scratch config — the real route needs adding to the external `e-com-app-config` repo (not part of this working directory) before it works outside local testing, same constraint as every other route in this gateway.

### Phase 7 — Seller-aware saga & payouts — done 2026-08-15

- ✅ `sellerId` threaded through the whole saga: `ProductPurchaseResponseDTO` (product-service) → each service's own `PurchaseResponseDTO` copy (order-service, and a newly-added one in payment-service, which previously had no `products` field on its `StockReservationResultEventDTO` at all — it only knew the order total, not what was in it). order-service's `StockReservationConsumer` backfills `OrderLine.sellerId` once stock reservation resolves (order lines are created eagerly at request time from just `{variantId, quantity}` — order-service can't know the seller until product-service resolves it).
- ✅ **`SellerPayout` ledger** (payment-service, new entity/table): one row per seller per order, computed the moment a Stripe charge succeeds (`StockReservationConsumer` → `SellerPayoutService.recordPayoutsForOrder`). Groups the order's lines by `sellerId`, sums `price × quantity` per seller, applies a flat platform commission (`platform.commission-rate`, default 10%), stores gross/commission/net. `GET /api/v1/payouts` — sellers see only their own (forced off `X-User-Id`, not a spoofable query param), admins see all.
- ✅ **Stripe Connect onboarding** (seller-service): `POST /api/v1/sellers/me/stripe/onboarding-link` creates an Express account on first call (`Account.create`, capabilities: transfers + card_payments) and returns a fresh `AccountLink` URL every call after. `POST /api/v1/sellers/webhooks/stripe` verifies Stripe's signature (`Webhook.constructEvent`) and updates `chargesEnabled`/`payoutsEnabled` from `account.updated` events — this is the only thing that flips those flags; nothing else touches them.
- ✅ **Settlement** (payment-service): `SellerPayoutService.settlePendingPayouts()` — one method, two triggers (`POST /api/v1/payouts/settle`, admin-only; and a daily `@Scheduled` job, `platform.payout-settlement-cron`). For each `PENDING` payout: looks up the seller via a new Feign client (`SellerClient`, calls seller-service directly — not through api-gateway, same pattern as order-service's `CustomerClient`); if `payoutsEnabled` is false or there's no Stripe account, marks `FAILED` with a reason; otherwise calls `Transfer.create` (`StripeTransferService`) and marks `PAID`/`FAILED` with the transfer id or failure reason. A seller-lookup failure (seller-service down, etc.) leaves the payout `PENDING` for the next run rather than failing the whole batch.
- ✅ 28 new/updated tests across product-service, order-service, payment-service, and seller-service — all passing, including the full commission-split math, the three settlement outcomes (paid/failed-no-onboarding/failed-transfer-error), and the "don't crash the batch on one bad lookup" behavior.

**Known simplifications, not oversights:**
- No end-to-end verification against real Stripe Connect test-mode accounts — `STRIPE_SECRET_KEY` is unset in this environment (same pre-existing limitation as Phase 2's charge flow), so `Account.create`/`AccountLink.create`/`Transfer.create` are only exercised through unit tests with `StripeConnectService`/`StripeTransferService` mocked out, not a live call. The saga-threading and ledger-math parts (the genuinely new logic) don't depend on a real key and are tested for real.
- Settlement currency is hardcoded `"usd"`, matching `StripePaymentServiceImpl`'s existing charge-side convention — no multi-currency support anywhere in the app yet.
- `SellerClient`'s base URL is a direct `http://localhost:8095/...` default (`application.config.seller-url`), same non-Eureka-discovery pattern `CustomerClient` already uses — not something this phase introduced.

### Phase 8 — Order lifecycle completeness
Not started. Seller fulfillment endpoint (tracking/`SHIPPED`/`DELIVERED`), refund flow (Stripe refund + `REFUNDED` + stock-release reuse), customer-scoped `GET /api/v1/orders/mine` + "My Orders" page, coupon codes.

### Phase 9 — Product images
Not started. MinIO in `docker-compose.yml`, `ProductImage` + presigned upload, seller-dashboard upload UI.

### Phase 10 — Reviews & ratings
Not started. Verified-purchase-gated `Review` entity in product-service, aggregate rating on PDP/cards.

### Phase 11 — Seller dashboard (frontend)
Not started. `frontend/src/app/seller/`: overview, products (CRUD + images), orders (fulfillment), payouts.

### Phase 12 — Admin dashboard (frontend)
Not started. `frontend/src/app/admin/`: categories, seller moderation, all-orders, Grafana links.

### Phase 13 — Observability: Prometheus + Grafana
Not started. `micrometer-registry-prometheus` on every service, business metrics at saga decision points, `prometheus`/`grafana`/`kafka-exporter` in `docker-compose.yml`.

### Phase 14 — SEO
Not started. `generateMetadata`, `sitemap.ts`/`robots.ts`, JSON-LD structured data.

### Phase 15 — Security & secrets hardening
Not started. Root `.env`/`.env.example` for all inline creds (Postgres/Mongo/Keycloak/pgAdmin/Stripe/Keycloak-admin-client-secret).

### Phase 16 — Testing & CI
Not started. Testcontainers for saga-critical paths, Playwright e2e, explicit `mvn verify` stage in each Jenkinsfile, frontend Jenkinsfile.

## 5. Decisions (locked 2026-08-15)

1. **Cart** — new `cart-service`, Redis-backed (§3 design: `cart:{userId}` hash).
2. **Payment provider** — real Stripe integration in `payment-service` (Phase 2 saga charges via Stripe, not a mock).
3. **Hosting target** — Docker Compose / single VM for production, matching the current dev setup. Phase 13/15 tooling (observability, secrets) is scoped to that, not Kubernetes.
4. **Category hierarchy** — flat `Category` for v1, no `parent_id`. Revisit only if the catalog actually grows multi-level.
5. **Marketplace model** — multi-vendor, not single-seller-admin. Sellers are Keycloak users holding a `seller` realm role (granted/revoked by `seller-service` via the Keycloak Admin API on approval/suspension), not a separate identity system.
6. **Seller scoping in the saga** — no order-splitting into multiple `Order` rows; a single checkout can mix products from multiple sellers, and `OrderLine` gains `sellerId` (Phase 7) rather than restructuring the saga around per-seller sub-orders.
7. **Payouts** — Stripe Connect "separate charges and transfers": one platform charge per order (unchanged), then per-seller `Transfer`s computed off a flat commission rate (Phase 7), not real-time split payments.
8. **Trust boundary** — `api-gateway` is the only service that validates JWTs; downstream services authorize off gateway-forwarded `X-User-Id`/`X-User-Roles` headers rather than each re-implementing OAuth2 resource-server config.
9. **Object storage** — MinIO (self-hosted, local) for product images (Phase 9), not a live cloud bucket.
