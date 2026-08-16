package com.rajitha.ecommerce.client.keycloak;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;

// Grants/revokes the "seller" realm role via Keycloak's Admin REST API, using this
// service's own confidential client (client-credentials grant) — the runtime
// equivalent of what keycloak/setup-nextjs-client.sh does as a one-off script for
// client provisioning. Requires the "seller-service" client (see
// keycloak/setup-seller-service-client.sh) to have the realm-management
// "manage-users" client role on its service account.
@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakRoleClient {

    private final KeycloakAdminProperties properties;
    private final RestClient restClient = RestClient.create();

    public void grantSellerRole(String keycloakUserId) {
        modifyRoleMapping(keycloakUserId, HttpMethod.POST);
        log.info("Granted realm role '{}' to Keycloak user {}", properties.sellerRole(), keycloakUserId);
    }

    public void revokeSellerRole(String keycloakUserId) {
        modifyRoleMapping(keycloakUserId, HttpMethod.DELETE);
        log.info("Revoked realm role '{}' from Keycloak user {}", properties.sellerRole(), keycloakUserId);
    }

    private void modifyRoleMapping(String keycloakUserId, HttpMethod method) {
        var adminToken = fetchAdminToken();
        var role = fetchSellerRole(adminToken);

        // Built via URI.create(...) on a plain concatenated string, not RestClient's
        // "{var}" template substitution — passing a full "http://host:port" value as
        // a template variable makes UriComponentsBuilder treat it as an opaque path
        // segment and percent-encode its ':'/'/' (and, with an IPv6 literal host,
        // even '['/']'), turning a valid base URL into a broken one.
        restClient.method(method)
                .uri(URI.create(properties.baseUrl() + "/admin/realms/" + properties.realm()
                        + "/users/" + keycloakUserId + "/role-mappings/realm"))
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
    }

    private String fetchAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());

        var response = restClient.post()
                .uri(URI.create(properties.baseUrl() + "/realms/" + properties.realm() + "/protocol/openid-connect/token"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KeycloakTokenResponse.class);

        if (response == null) {
            throw new IllegalStateException("Keycloak did not return an admin token — check keycloak.admin.* config");
        }
        return response.accessToken();
    }

    private KeycloakRoleRepresentation fetchSellerRole(String adminToken) {
        var role = restClient.get()
                .uri(URI.create(properties.baseUrl() + "/admin/realms/" + properties.realm()
                        + "/roles/" + properties.sellerRole()))
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .body(KeycloakRoleRepresentation.class);

        if (role == null) {
            throw new IllegalStateException(
                    "Keycloak realm role '" + properties.sellerRole() + "' not found — run keycloak/setup-realm-roles.sh first");
        }
        return role;
    }
}
