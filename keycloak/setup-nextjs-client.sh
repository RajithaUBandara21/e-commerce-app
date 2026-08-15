#!/usr/bin/env bash
# Provisions the public OAuth2 client the Next.js frontend authenticates with,
# against the "micro" realm already running in the docker-compose Keycloak instance.
#
# Nothing about the "micro" realm itself lives in this repo (it was created by hand
# in the admin console, same as the realm api-gateway already validates JWTs against)
# — this script is the versioned, repeatable replacement for doing this one step by
# hand too. Safe to re-run: it checks for an existing client before creating one.
#
# Usage:
#   KEYCLOAK_URL=http://localhost:8080 \
#   KEYCLOAK_ADMIN_USER=admin \
#   KEYCLOAK_ADMIN_PASSWORD=admin \
#   ./setup-nextjs-client.sh

set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
REALM="${KEYCLOAK_REALM:-micro}"
ADMIN_USER="${KEYCLOAK_ADMIN_USER:-admin}"
ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
CLIENT_ID="${NEXTJS_CLIENT_ID:-nextjs-storefront}"
FRONTEND_ORIGIN="${FRONTEND_ORIGIN:-http://localhost:3000}"

echo "Requesting admin token from ${KEYCLOAK_URL} ..."
ADMIN_TOKEN=$(curl -sf \
  -d "client_id=admin-cli" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASSWORD}" \
  -d "grant_type=password" \
  "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

if [ -z "${ADMIN_TOKEN}" ]; then
  echo "Failed to obtain an admin token. Check KEYCLOAK_URL/KEYCLOAK_ADMIN_USER/KEYCLOAK_ADMIN_PASSWORD." >&2
  exit 1
fi

EXISTING=$(curl -sf \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  "${KEYCLOAK_URL}/admin/realms/${REALM}/clients?clientId=${CLIENT_ID}")

if [ "${EXISTING}" != "[]" ]; then
  echo "Client '${CLIENT_ID}' already exists in realm '${REALM}' — nothing to do."
  exit 0
fi

echo "Creating public client '${CLIENT_ID}' in realm '${REALM}' ..."
curl -sf -X POST \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  "${KEYCLOAK_URL}/admin/realms/${REALM}/clients" \
  -d @- <<EOF
{
  "clientId": "${CLIENT_ID}",
  "publicClient": true,
  "protocol": "openid-connect",
  "standardFlowEnabled": true,
  "directAccessGrantsEnabled": false,
  "implicitFlowEnabled": false,
  "serviceAccountsEnabled": false,
  "redirectUris": ["${FRONTEND_ORIGIN}/*"],
  "webOrigins": ["${FRONTEND_ORIGIN}"],
  "attributes": {
    "pkce.code.challenge.method": "S256",
    "post.logout.redirect.uris": "${FRONTEND_ORIGIN}/*"
  }
}
EOF

echo "Done. '${CLIENT_ID}' is ready for NextAuth's Keycloak provider (authorization code + PKCE, no client secret)."
