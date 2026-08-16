package com.rajitha.ecommerce.client.keycloak;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record KeycloakRoleRepresentation(String id, String name) {
}
