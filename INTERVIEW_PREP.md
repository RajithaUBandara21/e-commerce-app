# Interview prep: defending this repo

`ARCHITECTURE.md` gets you the interview. This doc is what gets you through it —
the questions someone technical is actually likely to ask about specific
decisions in this repo, with the reasoning trail and where it lives in the code.

**How to use this**: don't memorize the answers below. Read the reasoning, then
close this file and explain it out loud in your own words. If you can't, go
re-read the relevant code — the goal is to be able to defend every decision
cold, not to recite a script.

---

### "Why does the payment-failure compensation reuse `stock-release-topic` instead of a dedicated topic?"

Because a refund needs to do the exact same thing to inventory that a
payment-failure compensation does: put reserved stock back. Two different
triggers (an async saga failure vs. a synchronous customer/admin refund
request), one consumer, one place the "how do we restore
`ProductVariant.availableQuantity`" logic lives. A separate topic would mean a
second consumer with the same logic, which is exactly the kind of duplication
that drifts out of sync over time.

*Where it lives*: `ARCHITECTURE.md#failure`, `CLAUDE.md`'s "Checkout saga"
section, `product-service`'s `StockReleaseConsumer`.

### "Why is Testcontainers only on order-service, not product-service and payment-service too?"

Two reasons, be honest about both. First, order-service is the saga's
orchestrator — it's the service where the "does the wiring between consumers
actually work" question matters most, so it's where the pattern got proven
first. Second, time budget: extending the same `@DynamicPropertySource` (real
Postgres + Kafka) + `@MockitoBean` (mock out the one synchronous cross-service
call) template to the other two services is mechanical, not a redesign — it
just wasn't done in this pass. If asked "would you extend it," the answer is
yes, and you should be able to sketch what that test would assert for each
service.

*Where it lives*: `ARCHITECTURE.md#testing`, `order-service/.../saga/OrderSagaIntegrationTest.java`.

### "Why does the gateway strip inbound `X-User-Id`/`X-User-Roles` before setting its own?"

Because those headers are how every downstream service decides who's calling
and what they're allowed to do — if a client could set them directly on a
request straight to `product-service` (bypassing the gateway entirely, which
is possible since nothing stops a direct network call in this local topology),
they could claim to be any user or any role. Stripping first means the *only*
way those headers can carry a value is if the gateway itself set them, which
only happens after a JWT was actually validated.

*Where it lives*: `ARCHITECTURE.md#architecture` (trust boundary diagram),
`UserContextGatewayFilter`.

### "Why no order-splitting when a cart has products from multiple sellers?"

Because it would have meant restructuring the saga around per-seller
sub-orders — a much bigger change — for a benefit (cleaner per-seller order
records) that a denormalized `OrderLine.sellerId` already delivers. One
`Order`, one Stripe charge, one payment record; `SellerPayout` does the
per-seller accounting at the ledger level instead. This is a real tradeoff,
not a free lunch: it does mean a single failed payment affects every seller in
that cart, not just one. Be ready to say that's the tradeoff and why it was
accepted (the accounting need is met without a saga rewrite).

*Where it lives*: `ARCHITECTURE.md#data`, `PLAN.md`'s locked decisions section.

### "Tell me about a real bug you found while building this."

Have two or three ready, not memorized verbatim, but understood well enough to
explain the *mechanism*, not just the headline:

- **The gateway 401-ing its own Prometheus scrape**: `anyExchange().authenticated()`
  applies to the gateway's own local endpoints, not just proxied traffic — a
  security catch-all with a blind spot. Fix: an explicit `permitAll()` matcher
  ahead of it. The lesson worth articulating: a security rule scoped as "every
  request" needs to account for requests *to* the service, not just *through*
  it.
- **The missing `stripePaymentIntentId`**: a refund needs to reference the
  original charge, and that id was never persisted despite the charge result
  always carrying it. Found before it shipped, while building refunds — a good
  example of "building the next feature surfaced a latent gap in the last one."
- **The circuit-breaker false-positive**: a seller-registration call actually
  succeeded on the backend, but a slow response tripped the gateway's circuit
  breaker before the client got the real answer, so the client saw a `503`
  fallback. Caught because the retry returned `409 "already exists"` instead of
  the expected success — the *symptom* revealed the request had, in fact,
  landed. Good one for "tell me about debugging something confusing," since the
  fix wasn't in the code that failed, it was in recognizing what the error
  actually meant.

*Where it lives*: `ARCHITECTURE.md#failure`, `PLAN.md`'s phase writeups (search
for "Real bugs hit").

### "Why Kafka choreography instead of a central saga orchestrator?"

Choreography means no single service owns the whole saga's state machine —
each service reacts to events and publishes its own, which keeps services
decoupled (product-service doesn't need to know payment-service exists) but
makes the *overall* flow harder to see in one place (there's no single
"OrderSagaOrchestrator" class to read — you have to trace it through topics).
That tradeoff is explicit here: `ARCHITECTURE.md`'s saga diagram exists
specifically because choreography's downside is discoverability, and a
document is a cheaper fix for that than restructuring into orchestration.

*Where it lives*: `ARCHITECTURE.md#architecture`, `CLAUDE.md`'s "Checkout saga"
section.

### "What happens if two customers buy the last unit of the same variant at the same time?"

Optimistic locking (`@Version` on `ProductVariant`). Both requests read the
same version; whichever writes first wins and increments the version; the
second write fails with a version conflict, which the service layer turns into
a clear `ProductPurchaseException` ("changed concurrently, please retry")
rather than a raw 500 or a silent oversell. Know the alternative you didn't
pick and why: a `SELECT ... FOR UPDATE` pessimistic lock would also work but
serializes all writes to that row even when there's no real contention —
optimistic locking only pays a cost when a conflict actually happens.

*Where it lives*: `ARCHITECTURE.md#requirements`, `ProductVariant.version`,
`ProductServiceIMPL.purchaseProductService`.

### "This is all local Docker Compose — what would you change for real production scale?"

Be honest that this wasn't the goal, and be specific about what *would* change,
not vague: Kubernetes (or at least multiple replicas of the stateless
services) instead of one VM; a managed Kafka/Postgres instead of
self-hosted containers holding all state on one disk; the at-least-once
Kafka delivery gap (no consumer-side dedup yet) would need closing before
real money moved through it at volume; Alertmanager instead of
dashboards-only observability. The point isn't that none of this was
considered — `ARCHITECTURE.md` states the Docker Compose / single-VM scope as
a deliberate decision, not a limitation nobody thought about.

*Where it lives*: `ARCHITECTURE.md#deployment`, `ARCHITECTURE.md#failure`
(the at-least-once dedup gap).

### "Walk me through what happens when a checkout's payment fails."

Be able to narrate the full path without looking: order created as
`PENDING_PAYMENT` → `order-created-topic` → product-service reserves stock,
publishes to `stock-topic` → payment-service (also listening on `stock-topic`)
attempts the Stripe charge, fails (or fails closed if no key is configured) →
publishes to `payment-topic` → order-service's `PaymentResultConsumer` sets
`Order.status = PAYMENT_FAILED` *and* publishes to `stock-release-topic` →
product-service's `StockReleaseConsumer` adds the reserved quantity back. This
is the single most important flow in the repo to be able to draw from memory.

*Where it lives*: `ARCHITECTURE.md#failure`'s sequence diagram — this is
literally what it draws.
