package com.rajitha.apigateway.config;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


// Role-based routing here relies on api-gateway being the ONLY place that validates
// the JWT — downstream services (product-service, seller-service, ...) trust the
// X-User-Id/X-User-Roles headers UserContextGatewayFilter forwards, rather than
// re-validating the token themselves. See PLAN.md's gateway-as-trust-boundary
// decision for why: it avoids duplicating OAuth2 resource-server config into every
// service just to answer "does this caller own this resource / hold this role".
@Configuration
@EnableWebFluxSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

//    private final JWTAuthConverter jwtAuthConverter;

    @Value("${application.config.frontend-origin:http://localhost:3000}")
    private String frontendOrigin;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity serverHttpSecurity) throws Exception {

        serverHttpSecurity
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchange ->exchange
                        .pathMatchers("/eureka/**")
                        .permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/products/**")
                        .permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/categories/**")
                        .permitAll()
                        // Seller/category mutations and admin-only seller-management
                        // endpoints. Order matters here: these specific matchers must
                        // come before the anyExchange() catch-all below. Anything
                        // seller-related NOT listed here (register, /me) just falls
                        // through to "authenticated" — any signed-in user, since
                        // becoming a seller has to be reachable before you hold
                        // ROLE_SELLER.
                        .pathMatchers(HttpMethod.PATCH, "/api/v1/sellers/*/status")
                        .hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.GET, "/api/v1/sellers")
                        .hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/v1/products")
                        .hasAnyAuthority("ROLE_SELLER", "ROLE_ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/v1/products/**")
                        .hasAnyAuthority("ROLE_SELLER", "ROLE_ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/products/**")
                        .hasAnyAuthority("ROLE_SELLER", "ROLE_ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/v1/categories")
                        .hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/v1/categories/**")
                        .hasAuthority("ROLE_ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/categories/**")
                        .hasAuthority("ROLE_ADMIN")
                        .anyExchange()
                        .authenticated())
                .oauth2ResourceServer(oauth2 ->oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return serverHttpSecurity.build();
    }

    // Keycloak puts realm roles under the "realm_access.roles" claim — Spring
    // Security's default JWT converter has no idea that claim exists, so
    // hasAuthority("ROLE_...") above would silently never match without this.
    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        var delegate = new JwtAuthenticationConverter();
        delegate.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return new ReactiveJwtAuthenticationConverterAdapter(delegate);
    }

    private List<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof List<?> roles)) {
            return List.of();
        }
        return roles.stream()
                .map(Object::toString)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
