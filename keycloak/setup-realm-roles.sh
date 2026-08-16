#!/usr/bin/env bash
# Creates the realm roles the marketplace relies on — customer/seller/admin — in the
# "micro" realm. Idempotent: skips any role that already exists.
#
# Run this once against a fresh Keycloak before:
#   - seller-service's grantSellerRole/revokeSellerRole (updateStatus flow) — it looks
#     up the "seller" role by name and fails loudly if it's missing.
#   - api-gateway's role-based path authorization means anything (ROLE_SELLER/ROLE_ADMIN
#     checks are no-ops if no user ever holds those realm roles).
#
# Usage:
#   KEYCLOAK_URL=http://localhost:8080 \
#   KEYCLOAK_ADMIN_USER=admin \
#   KEYCLOAK_ADMIN_PASSWORD=admin \
#   ./setup-realm-roles.sh

set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
REALM="${KEYCLOAK_REALM:-micro}"
ADMIN_USER="${KEYCLOAK_ADMIN_USER:-admin}"
ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"

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

create_role_if_missing() {
  local role_name="$1"
  local description="$2"

  local status
  status=$(curl -s -o /dev/null -w '%{http_code}' \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    "${KEYCLOAK_URL}/admin/realms/${REALM}/roles/${role_name}")

  if [ "${status}" = "200" ]; then
    echo "Role '${role_name}' already exists — skipping."
    return
  fi

  echo "Creating role '${role_name}' ..."
  curl -sf -X POST \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    "${KEYCLOAK_URL}/admin/realms/${REALM}/roles" \
    -d "{\"name\": \"${role_name}\", \"description\": \"${description}\"}"
}

create_role_if_missing "customer" "Default role for authenticated storefront customers"
create_role_if_missing "seller" "Granted by seller-service once a seller profile is approved (status -> ACTIVE)"
create_role_if_missing "admin" "Platform administrator: category/seller moderation, all-orders visibility"

echo "Done. Roles are ready for api-gateway's role-based routing and seller-service's role grants."
