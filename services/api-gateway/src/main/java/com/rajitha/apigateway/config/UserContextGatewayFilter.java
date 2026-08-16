package com.rajitha.apigateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

// Forwards the caller's identity downstream as plain headers, since internal
// services trust the gateway rather than each re-validating the JWT themselves
// (see SecurityConfiguration's javadoc / PLAN.md's gateway-as-trust-boundary
// decision). Any X-User-Id/X-User-Roles already on the INCOMING request is
// stripped first so a client can't spoof them — only what this filter derives
// from the verified JWT is ever forwarded.
@Component
public class UserContextGatewayFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var strippedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_ROLES_HEADER);
                })
                .build();
        var strippedExchange = exchange.mutate().request(strippedRequest).build();

        // exchange.getPrincipal() is not reliably populated by Spring Security
        // WebFlux at the point a Gateway GlobalFilter runs; ReactiveSecurityContextHolder
        // (backed by the Reactor Context Security writes into) is the mechanism that's
        // actually guaranteed to carry the authenticated principal through here.
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .map(jwt -> withUserContext(strippedExchange, jwt))
                .defaultIfEmpty(strippedExchange)
                .flatMap(chain::filter);
    }

    private ServerWebExchange withUserContext(ServerWebExchange exchange, Jwt jwt) {
        var request = exchange.getRequest().mutate()
                .header(USER_ID_HEADER, jwt.getSubject())
                .header(USER_ROLES_HEADER, String.join(",", extractRoles(jwt)))
                .build();
        return exchange.mutate().request(request).build();
    }

    private List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof List<?> roles)) {
            return List.of();
        }
        return roles.stream().map(Object::toString).toList();
    }

    // Must run before NettyRoutingFilter (which proxies the request downstream) so
    // the mutated headers are actually present on the outbound call. Comfortably
    // ahead of that without needing to reason about exact GlobalFilter ordering.
    @Override
    public int getOrder() {
        return -1;
    }
}
