package com.rajitha.ecommerce.client.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record KeycloakTokenResponse(@JsonProperty("access_token") String accessToken) {
}
