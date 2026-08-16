#!/usr/bin/env bash
# Provisions a confidential, service-account-only client ("seller-service" by default)
# that seller-service uses to call Keycloak's Admin REST API (client-credentials grant)
# and grant/revoke the "seller" realm role as sellers are approved/suspended — see
# KeycloakRoleClient.java. Grants the service account the realm-management
# "manage-users", "view-users", and "view-realm" client roles: manage-users/view-users
# to look up a user and modify their realm role mappings, view-realm because reading a
# realm role's representation (GET /admin/realms/{realm}/roles/{name}, needed before a
# role-mapping call) is gated separately and 403s without it even with manage-users alone.
#
# Run setup-realm-roles.sh first — this script does not create the "seller" role itself.
# Safe to re-run: skips client creation if "seller-service" already exists, but still
# prints its current secret so you can copy it into KEYCLOAK_ADMIN_CLIENT_SECRET.
#
# Usage:
#   KEYCLOAK_URL=http://localhost:8080 \
#   KEYCLOAK_ADMIN_USER=admin \
#   KEYCLOAK_ADMIN_PASSWORD=admin \
#   ./setup-seller-service-client.sh

set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8080}"
REALM="${KEYCLOAK_REALM:-micro}"
ADMIN_USER="${KEYCLOAK_ADMIN_USER:-admin}"
ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"
CLIENT_ID="${SELLER_SERVICE_CLIENT_ID:-seller-service}"

json_field() {
  # Extracts a top-level "key":"value" or "key":value string/number field from a
  # single-line JSON blob — matches the parsing style already used in
  # setup-nextjs-client.sh (no jq dependency assumed).
  local json="$1" key="$2"
  echo "${json}" | grep -o "\"${key}\":\"[^\"]*\"" | head -1 | cut -d'"' -f4
}

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

auth_get() {
  curl -sf -H "Authorization: Bearer ${ADMIN_TOKEN}" "$@"
}

EXISTING=$(auth_get "${KEYCLOAK_URL}/admin/realms/${REALM}/clients?clientId=${CLIENT_ID}")

if [ "${EXISTING}" = "[]" ]; then
  echo "Creating confidential service-account client '${CLIENT_ID}' in realm '${REALM}' ..."
  curl -sf -X POST \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    "${KEYCLOAK_URL}/admin/realms/${REALM}/clients" \
    -d "{
      \"clientId\": \"${CLIENT_ID}\",
      \"publicClient\": false,
      \"protocol\": \"openid-connect\",
      \"standardFlowEnabled\": false,
      \"directAccessGrantsEnabled\": false,
      \"implicitFlowEnabled\": false,
      \"serviceAccountsEnabled\": true
    }"
else
  echo "Client '${CLIENT_ID}' already exists — checking role mappings and secret."
fi

CLIENT_UUID=$(json_field "$(auth_get "${KEYCLOAK_URL}/admin/realms/${REALM}/clients?clientId=${CLIENT_ID}")" "id")
if [ -z "${CLIENT_UUID}" ]; then
  echo "Could not resolve internal id for client '${CLIENT_ID}'." >&2
  exit 1
fi

SERVICE_ACCOUNT_USER=$(auth_get "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${CLIENT_UUID}/service-account-user")
SERVICE_ACCOUNT_USER_ID=$(json_field "${SERVICE_ACCOUNT_USER}" "id")

REALM_MGMT_CLIENT_UUID=$(json_field "$(auth_get "${KEYCLOAK_URL}/admin/realms/${REALM}/clients?clientId=realm-management")" "id")

grant_client_role_if_missing() {
  local role_name="$1"

  local current_roles
  current_roles=$(auth_get "${KEYCLOAK_URL}/admin/realms/${REALM}/users/${SERVICE_ACCOUNT_USER_ID}/role-mappings/clients/${REALM_MGMT_CLIENT_UUID}")
  if echo "${current_roles}" | grep -q "\"name\":\"${role_name}\""; then
    echo "Service account already has client role '${role_name}' — skipping."
    return
  fi

  local role_representation
  role_representation=$(auth_get "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${REALM_MGMT_CLIENT_UUID}/roles/${role_name}")

  echo "Granting realm-management role '${role_name}' to seller-service's service account ..."
  curl -sf -X POST \
    -H "Authorization: Bearer ${ADMIN_TOKEN}" \
    -H "Content-Type: application/json" \
    "${KEYCLOAK_URL}/admin/realms/${REALM}/users/${SERVICE_ACCOUNT_USER_ID}/role-mappings/clients/${REALM_MGMT_CLIENT_UUID}" \
    -d "[${role_representation}]"
}

grant_client_role_if_missing "manage-users"
grant_client_role_if_missing "view-users"
grant_client_role_if_missing "view-realm"

CLIENT_SECRET=$(json_field "$(auth_get "${KEYCLOAK_URL}/admin/realms/${REALM}/clients/${CLIENT_UUID}/client-secret")" "value")

echo ""
echo "Done. Set these for seller-service:"
echo "  KEYCLOAK_ADMIN_CLIENT_ID=${CLIENT_ID}"
echo "  KEYCLOAK_ADMIN_CLIENT_SECRET=${CLIENT_SECRET}"
