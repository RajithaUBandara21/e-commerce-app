# Deploying to an Oracle Cloud Always-Free VM

This is the runbook for standing up the full stack (all 10 backend
services + frontend + infra + observability) on a single VM using
`docker-compose.prod.yml`. It assumes the VM already exists — see the
Oracle Cloud console steps you were given separately (create a
VM.Standard.A1.Flex instance, 4 OCPU / 24GB RAM, Ubuntu 22.04+, open ports
22/80/443/3000/8222/8080/9000 in the subnet's Security List).

## 1. Install Docker on the VM

```bash
ssh ubuntu@<VM_PUBLIC_IP>

curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker $USER
newgrp docker

# Also need a JDK + Maven on the VM itself — the service Dockerfiles copy a
# pre-built jar (COPY target/*.jar app.jar), they don't build it in-image.
sudo apt-get update
sudo apt-get install -y openjdk-21-jdk maven git
```

## 2. Get the code onto the VM

```bash
git clone <your-repo-url> e-commerce-app
cd e-commerce-app
```

## 3. Build every service's jar

```bash
for s in customer-service product-service seller-service order-service \
         payment-service notification-service cart-service api-gateway; do
  (cd "services/$s" && mvn -q package -DskipTests)
done
```

(`discovery-service`/`config-server` are intentionally not built or run —
see CLAUDE.md, everything else tolerates them being absent.)

## 4. Configure environment

```bash
cp .env.prod.example .env.prod
```

Edit `.env.prod`:
- `DEPLOY_HOST` — the VM's public IP (or a domain once you have one)
- `AUTH_SECRET` — generate one: `npx auth secret` (needs Node — or just use
  `openssl rand -base64 32`)
- Leave `KEYCLOAK_ADMIN_CLIENT_SECRET`/`STRIPE_*` blank for now — filled in
  after Keycloak is up (next step).

## 5. Bring the stack up

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

First run pulls images and builds 11 containers — expect several minutes.
Watch `docker compose -f docker-compose.prod.yml logs -f api-gateway` for
the gateway to report a clean startup once the rest is up.

**If Kafka or MinIO images fail to pull** on the ARM VM (community images
occasionally lag on multi-arch support), that's the one thing this compose
file couldn't be verified against ahead of time — see CLAUDE.md's note on
this. Swap the failing image for a confirmed-arm64 alternative and retry.

## 6. Provision Keycloak (this is a *brand new* Keycloak — nothing carries
   over from local dev)

```bash
cd keycloak
export KEYCLOAK_URL="http://<DEPLOY_HOST>:8080"
export KEYCLOAK_ADMIN_USER=admin KEYCLOAK_ADMIN_PASSWORD=admin
./setup-realm-roles.sh
./setup-nextjs-client.sh          # also update its redirect URI —see below
./setup-seller-service-client.sh  # prints a client secret, copy it
```

Then:
- In Keycloak's admin console (`http://<DEPLOY_HOST>:8080`), open the
  `nextjs-storefront` client and set **Valid redirect URIs** to
  `http://<DEPLOY_HOST>:3000/*` (the setup script defaults to localhost).
- Put the printed `seller-service` client secret into `.env.prod`'s
  `KEYCLOAK_ADMIN_CLIENT_SECRET`, then:
  ```bash
  docker compose -f docker-compose.prod.yml --env-file .env.prod up -d seller-service
  ```

## 7. Verify

```bash
curl http://<DEPLOY_HOST>:8222/api/v1/products      # empty [] until you add a product
curl http://<DEPLOY_HOST>:3000                        # frontend HTML
```

Visit `http://<DEPLOY_HOST>:3000` in a browser, sign in (Register a fresh
account — see PLATFORM_GUIDE for the account/role flow), walk the golden
path.

## Known limitations of this deployment (by design, not oversights)

- **HTTP, not HTTPS.** No TLS cert without a real domain. Fine for a
  portfolio demo; don't put real payment details through it — Stripe stays
  in test mode (`STRIPE_SECRET_KEY` empty by default, fails closed).
- **No domain name required**, everything's keyed off `DEPLOY_HOST` in
  `.env.prod` — if you do point a domain at the VM later, update that one
  value and re-run step 5's `up -d --build`.
- **discovery-service/config-server are not deployed** — routing is static
  (baked into `docker-compose.prod.yml`'s env vars), matching the
  already-established local-demo pattern from earlier in this project.
- **notification-service sends no real email** — MailDev isn't deployed
  here either; failed sends are logged, not fatal (see
  `PaymentConfirmationDTO.success()` gating in CLAUDE.md).
