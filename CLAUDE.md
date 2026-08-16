# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

A Spring Cloud e-commerce **multi-vendor marketplace** backend split into 10 independent Maven projects under `services/` (no multi-module parent — each service has its own `pom.xml` and is built/run separately):

- **discovery-service** — Eureka server; all other services register with it.
- **config-server** — Spring Cloud Config server, port 8888, backed by an external git repo (`e-com-app-config`) for centralized `application.yml` config per profile.
- **api-gateway** — Spring Cloud Gateway; OAuth2 resource server validating JWTs against Keycloak (`http://localhost:8080/realms/micro`). This is the entry point for external traffic, and the **only** place a JWT gets validated — see "Auth & roles" below.
- **customer-service** — customer + address data, stored in MongoDB (`document` package).
- **product-service** — product catalog, JPA/Postgres. A `Product` (name/description/price/category/**`sellerId`**) has many `ProductVariant`s (sku/size/color/`availableQuantity`, optimistic-locked via `@Version`) — variants are the purchasable/stock-tracked unit, not the product itself. Product mutations (`POST`/`PUT`/`DELETE /api/v1/products`) are ownership-checked against `sellerId` (admin bypasses). `Category` has full CRUD (public reads, admin-only writes) — deleting a non-empty category is rejected rather than cascading. Product photos live in MinIO (`ProductImage`, ordered): `POST .../images/upload-url` hands the browser a presigned MinIO PUT URL, the browser uploads directly, `POST .../images` registers the result — image bytes never pass through this service. `ProductResponseDTO.imageUrls` is public (bucket has a public-read policy). `Review` (`POST`/`GET`/`DELETE /api/v1/products/{id}/reviews`, one per customer per product) is gated on verified purchase — a Feign call (`client/feign/OrderClient`, this service's only Feign client) to order-service's internal `purchased-variants` endpoint, cross-referenced against the product's own variant ids; fails closed if order-service is unreachable. `ProductResponseDTO.averageRating`/`reviewCount` are computed from `Review` rows, `null`/`0` (no badge shown) rather than fabricated for unreviewed products.
- **seller-service** — seller identity/profile, JPA/Postgres. `Seller` (`keycloakUserId`, business profile, `SellerStatus` PENDING/ACTIVE/SUSPENDED, Stripe Connect fields). Approving a seller (`PATCH /api/v1/sellers/{id}/status` → `ACTIVE`) calls Keycloak's Admin REST API (`KeycloakRoleClient`, client-credentials grant using this service's own confidential client) to grant the `seller` realm role; suspending revokes it. `POST /api/v1/sellers/me/stripe/onboarding-link` creates a Stripe Express Connect account on first call and returns an `AccountLink` URL every call; `POST /api/v1/sellers/webhooks/stripe` (signature-verified, not JWT-authenticated — Stripe calls it directly) is the only thing that flips `chargesEnabled`/`payoutsEnabled` from `account.updated` events. `GET /api/v1/sellers/lookup/{keycloak-user-id}` is service-to-service only (Feign, not through the gateway) — payment-service calls it during payout settlement.
- **order-service** — order + order-line orchestration, JPA/Postgres. `POST /api/v1/orders` only validates the customer (still a synchronous OpenFeign call via `client/feign/CustomerClient`) and persists the order as `PENDING_PAYMENT`; stock reservation and payment are async from there (see Checkout saga below). An optional `couponCode` on the request is validated/applied (`CouponService`) before the order is saved. No more direct HTTP calls to `product-service` — that client was removed when the saga replaced it. `client/feign/PaymentClient` is the one deliberate exception: a customer/admin-triggered refund (`POST /api/v1/orders/{id}/refund`) needs an immediate result, so it calls payment-service synchronously rather than through Kafka. `GET /api/v1/orders/mine` is customer-scoped (off `X-User-Id`); the unfiltered `GET /api/v1/orders` is gateway-gated to `ROLE_ADMIN`. `PATCH /api/v1/order-lines/{id}/fulfillment` (seller/admin) is what actually moves `Order.status` to `SHIPPED`/`DELIVERED` — derived from each line's own fulfillment status, not set directly. `GET /api/v1/order-lines/purchased-variants/{customer-id}` is internal/service-to-service only (not through the gateway, no header trust — caller supplies `customerId` directly): product-service's review feature calls it to check verified-purchase eligibility.
- **payment-service** — payment records, JPA/Postgres. Charges via Stripe (`StripePaymentService`/`StripePaymentServiceImpl`, `com.stripe:stripe-java`) triggered off the saga; `stripe.secret-key` reads `${STRIPE_SECRET_KEY}` (empty by default — charges fail closed with "No Stripe payment method provided" until a real key and a `stripePaymentMethodId` from a frontend are wired up). `POST /api/v1/payments` (`PaymentController`/`PaymentServiceImpl.createPayment`) still exists as a separate synchronous "always succeeds, no Stripe" entry point for direct/admin use — it does not participate in the saga. On a successful charge, also writes one `SellerPayout` ledger row per seller in the order (`SellerPayoutService.recordPayoutsForOrder` — gross/commission/net, `platform.commission-rate`, default 10%). `SellerPayoutService.settlePendingPayouts()` actually moves the money (Stripe `Transfer` to each seller's Connect account, via a Feign call to seller-service for their `stripeAccountId`) — triggered by `POST /api/v1/payouts/settle` (admin) or a daily `@Scheduled` job (`PayoutSettlementScheduler`). `POST /api/v1/payments/refund` (called by order-service, not the browser) issues a Stripe refund against the charge's `stripePaymentIntentId` — persisted on `Payment` since Phase 8; it wasn't before, despite `StripePaymentService.ChargeResult` always having carried it.
- **notification-service** — consumes Kafka events (`messaging/consumer/NotificationConsumer`) to send emails (Spring Mail + Thymeleaf templates, tested locally against MailDev). Stores notifications in MongoDB (`document` package). Payment emails are guarded on `PaymentConfirmationDTO.success()` — a failed payment is logged, not emailed.
- **cart-service** — Redis-backed cart (`repository/CartRepository`, a `cart:{userId}` hash keyed by variant id, no JPA/Mongo). `POST /api/v1/cart/{userId}/checkout` calls `order-service` via Feign (`client/feign/OrderClient`) and clears the cart on success.

Every service follows the same package layout: `controller`, `service` + `service/serviceImpl` (interface/impl split), `repository`, `entity` or `document` (JPA vs MongoDB), `dto`, `mapper`, `handler` (`GlobalExceptionHandler`), `exception`/`exeption` (spelling varies by service — check before creating a new one).

### Auth & roles (Keycloak, multi-vendor)

Three realm roles in `micro`: `customer` (default), `seller`, `admin` — provisioned by `keycloak/setup-realm-roles.sh` (idempotent; run once per environment). `api-gateway`'s `SecurityConfiguration` unpacks the JWT's `realm_access.roles` claim into `ROLE_*` Spring authorities (`JwtAuthenticationConverter`, not the default — Spring Security has no built-in support for Keycloak's nested roles claim) and gates mutation routes accordingly (`ROLE_ADMIN` for category/seller-admin endpoints, `ROLE_SELLER`/`ROLE_ADMIN` for product mutations).

**Gateway is the only trust boundary**: downstream services do not validate JWTs themselves. `UserContextGatewayFilter` (a `GlobalFilter`) forwards `X-User-Id` (JWT `sub`) and `X-User-Roles` (comma-joined realm roles) to every downstream call, stripping any inbound values on those headers first so a client can't spoof them by calling a service directly. Product/category/seller controllers read these headers directly for ownership/admin checks — there's no Spring Security dependency in those services at all. If you add a new mutation endpoint anywhere, follow this pattern rather than adding OAuth2-resource-server config to the service.

Becoming a seller: `POST /api/v1/sellers/register` (any authenticated user) → `seller-service` creates a `PENDING` `Seller` row → an admin calls `PATCH /api/v1/sellers/{id}/status` with `ACTIVE` → `seller-service` calls Keycloak's Admin API to grant the `seller` role → the user's *next* token (not their current one — roles are baked in at token-issue time) carries `ROLE_SELLER`. `keycloak/setup-seller-service-client.sh` provisions the confidential client `seller-service` authenticates to Keycloak's Admin API as; it needs `manage-users`, `view-users`, **and** `view-realm` client roles from `realm-management` (the last one is easy to miss — reading a realm role's representation via `GET /admin/realms/{realm}/roles/{name}` 403s without it, even with the other two granted).

### Checkout saga (Kafka choreography)

`order-service` → `product-service` → `payment-service` → back to `order-service`, correlated by `orderReference` (a string) — **not** any numeric order id, since order-service's own id field can't be trusted as a correlation key (it's technically client-suppliable on `OrderRequestDTO`). Topics, in flow order:

1. `order-created-topic` — order-service publishes `OrderCreatedEventDTO` (customer, requested variants, amount, payment method, optional `stripePaymentMethodId`) right after saving the `PENDING_PAYMENT` order.
2. `stock-topic` — product-service's `OrderCreatedConsumer` reserves stock per variant (reusing `ProductServiceIMPL.purchaseProductService`, so the same optimistic-lock/insufficient-stock handling applies) and publishes success (echoing resolved product/variant details **and each line's `sellerId`** forward) or failure. order-service's `StockReservationConsumer` reacts: success → backfills `OrderLine.sellerId` from the echoed details (order lines are created eagerly at request time before any seller is known) and sends the order-confirmation email (`order-topic`, unchanged shape); failure → order → `CANCELLED`.
3. payment-service's own `StockReservationConsumer` (same topic, different service/package) reacts only to successes: charges via Stripe, records a `SellerPayout` ledger row per seller in the order on success, publishes to `payment-topic` either way (`success`/`reason` fields, `PaymentNotificationRequestDTO`/`PaymentConfirmationDTO`).
4. `payment-topic` — order-service's `PaymentResultConsumer` reacts: success → order → `CONFIRMED`; failure → order → `PAYMENT_FAILED` **and** publishes `StockReleaseEventDTO` to `stock-release-topic` so product-service's `StockReleaseConsumer` restores the reserved quantity (`ProductService.releaseStock`).

Each service keeps its own local copy of the shared event DTOs (no shared library between services — matches the existing per-service DTO duplication convention, e.g. `PaymentMethode`/`CustomerResponseDTO`). Field names must match what the *producer* actually emits, not just what the reader wants — Jackson will silently drop unmatched fields rather than error. There's no consumer-side dedup for redelivered Kafka messages yet (a known simplification, not an oversight — see `PLAN.md`).

Idempotency for the initial HTTP call: `POST /api/v1/orders` honors an `Idempotency-Key` header (unique-constrained on `Order.idempotencyKey`) — a repeated key returns the existing order id without repeating the customer check or re-publishing `OrderCreatedEventDTO`.

Tracing goes through Zipkin (`micrometer-tracing-bridge-brave`, port 9411) on services with `spring-boot-starter-actuator`.

## Frontend

`frontend/` is a separate Next.js 16 (App Router, Turbopack, TypeScript, Tailwind) app — not a Maven service, run with `npm run dev`/`npm run build` from inside `frontend/`. It's the only client of `api-gateway`; nothing else in the repo calls the gateway from outside.

- Auth: NextAuth v5 (`src/auth.ts`) with a Keycloak PKCE public client (see `keycloak/setup-nextjs-client.sh`, which must be run against a live Keycloak before sign-in works). `src/proxy.ts` (Next 16 renamed `middleware.ts` → `proxy.ts`) gates `/cart`, `/checkout`, `/account`, `/orders`, `/seller`. `session.roles` (decoded from the Keycloak access token's `realm_access.roles` claim in `auth.ts`'s `jwt` callback) is how the UI knows if the signed-in user is a seller.
- Data: `src/lib/api.ts` is the only place that calls the gateway; catalog reads are public (`GET /api/v1/products/**`, permitted at the gateway without a token), everything else needs the session's access token. Cart/checkout mutations go through Server Actions (`src/lib/actions.ts`), not client-side fetch.
- Seller dashboard: `src/app/seller/**` (`/seller` overview, `/products`, `/orders`, `/payouts`, `/onboarding`). Every page except `/onboarding` gates through `SellerStatusGate` (unregistered → CTA, `PENDING`/`SUSPENDED` → status message, `ACTIVE` → renders `SellerNav` + the page) — add new seller pages through that component rather than re-implementing the branch.
- Admin dashboard: `src/app/admin/**` (`/categories`, `/sellers`, `/orders`; `/admin` redirects to `/categories`). Gated by `AdminGate` (`session.roles.includes("admin")`) — a UI convenience only, since the gateway independently enforces `ROLE_ADMIN` on every write these pages make. No new backend endpoints — this phase only consumed what Phases 6/8 already exposed.
- If you touch `src/auth.ts` or `src/types/next-auth.d.ts`: TypeScript module augmentation for `next-auth`/`next-auth/jwt` is fragile here — see the "real bug hit and fixed" note in `PLAN.md`'s Phase 5 writeup before changing either file. The short version: the augmentation `.d.ts` needs its own `export {}`, and both `next-auth/jwt` and `@auth/core/jwt` need the same fields augmented.
- This Next.js version has real breaking changes from older training data (renamed `proxy.ts`, `fetch` no longer cached by default, `params`/`searchParams` are Promises). When in doubt, check `frontend/node_modules/next/dist/docs/` before assuming an API — `frontend/AGENTS.md` says the same.

## Local infrastructure

`docker-compose.yml` at the repo root brings up everything services depend on:

```
docker compose up -d
```

- Postgres `5432` (user/pass `rajitha`/`rajitha`) + pgAdmin `8087`
- MongoDB `27017` (user/pass `rajitha`/`rajitha`) + mongo-express `8081`
- Kafka `9092` (+ Zookeeper `22181`)
- Redis `6379` (cart-service)
- MailDev `10081` (UI) / `1025` (SMTP)
- Zipkin `9411`
- Keycloak `8080` (realm `micro`, admin/admin, `start-dev` mode)
- MinIO `9000` (API) / `9001` (console), product images — bucket `product-images` is auto-created with a public-read policy by product-service on startup

Service startup order matters: **config-server → discovery-service**, then the rest (each service does `optional:configserver:http://localhost:8888` on boot, and registers with Eureka).

## Common commands

Run per-service, from inside `services/<name>/` (there is no root aggregator POM):

```
mvn clean install       # build + test
mvn test                 # run all tests for this service
mvn test -Dtest=ClassName#methodName   # run a single test
mvn spring-boot:run      # run the service locally
```

Test layout mirrors main: mapper tests (e.g. `mapper/OrderMapperTest.java`) and service-impl tests (e.g. `service/serviceImpl/OrderServiceImplTest.java`) using Mockito/MockitoBean, plus one `*ApplicationTests` context-load test per service.

For the frontend, from inside `frontend/`: `npm run dev`, `npm run build`, `npm run lint`, `npx tsc --noEmit`. Copy `.env.local.example` to `.env.local` first.

## Gotchas

- `services/config-server/src/main/resources/application.yml` reads the config-repo git password from `${GITHUB_TOKEN}` now (previously hardcoded — the old token is still in git history and needs revoking on GitHub, that part isn't fixable from this repo).
- Package name for the exception folder is `exception` in most services but `exeption` (typo) in `customer-service` and `product-service` — match the existing spelling within a given service rather than "fixing" it as a drive-by.
- `Order.reference` and `Order.idempotencyKey` are both unique-constrained; `Order.reference` in particular is now load-bearing as the saga's correlation key across three services, not just a display string.
- Don't add stock-decrement/refund logic anywhere except `ProductServiceIMPL.purchaseProductService`/`releaseStock` — the saga's stock-topic/stock-release-topic consumers call straight into these, so that's the one place the "how do we mutate `ProductVariant.availableQuantity`" logic should live.
- **`exchange.getPrincipal()` doesn't reliably return the authenticated principal inside a Spring Cloud Gateway `GlobalFilter`**, even on a fully-authenticated request — it read empty consistently in testing. Use `ReactiveSecurityContextHolder.getContext().map(SecurityContext::getAuthentication)` instead (see `UserContextGatewayFilter`); that's backed by the Reactor `Context` Spring Security actually writes the authentication into.
- **`RestClient`/`WebClient`'s `.uri("{var}/path", value)` template substitution percent-encodes whatever `value` is**, including `:`/`/` and (for an IPv6 literal) `[`/`]` — passing a full `http://host:port` base URL as a template variable silently breaks it. Build the full URL as a string and pass `.uri(URI.create(fullUrl))` instead (see `seller-service`'s `KeycloakRoleClient`).
- Keycloak's `admin-cli` client issues near-empty access tokens for manual/scripted testing (no `sub`, no `realm_access`) — get a test token from a client with the default full client-scope set (e.g. `nextjs-storefront`) instead, or you'll chase phantom "missing X-User-Id" bugs that are actually just a bad test token.
- Gateway routing (`spring.cloud.gateway.routes`) lives entirely in the external `e-com-app-config` repo, not in this one — `services/api-gateway/src/main/resources/application.yml` defines zero routes. Adding a new service means adding its route there (or a local scratch override for testing); nothing here in the repo will route to it otherwise.
- **`services/product-service/src/main/resources/db/migration/` (Flyway `V1`/`V2`) is stale** — it predates the `ProductVariant`/`sellerId`/`Category`-CRUD/`ProductImage` schema entirely (still has the old `product.available_quantity` column, no `product_variant`/`product_image` tables). Every schema change this project has made relies on Hibernate `ddl-auto: update` instead; local scratch configs explicitly set `spring.flyway.enabled: false`. Don't assume the migration files reflect current schema, and don't add new ones without first reconciling the existing ones — they'd fail against a fresh database as-is.
