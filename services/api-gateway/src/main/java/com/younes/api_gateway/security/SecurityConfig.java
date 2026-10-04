package com.younes.api_gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${keycloak.issuer-uri}")
    private String issuerUri;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity httpSecurity) {
        httpSecurity
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchange -> exchange
                .pathMatchers("/actuator/health").permitAll()
                // Internal compensation endpoint, called by the order service through
                // lb://product-service when an order is rolled back. It must never be
                // reachable from outside: without this rule any authenticated user
                // could call it and inflate stock at will (verified: one call took a
                // product from 4 to 54 units).
                .pathMatchers(HttpMethod.POST, "/api/v1/products/purchase/release").denyAll()
                .pathMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                // Creating a customer profile now requires a token: the profile is
                // bound to the caller's Keycloak identity, so it cannot be anonymous.
                .pathMatchers(HttpMethod.POST, "/api/v1/customers").authenticated()
                // Listing every customer exposes all profiles, so it is ADMIN only.
                .pathMatchers(HttpMethod.GET, "/api/v1/customers").hasRole("ADMIN")
                .pathMatchers(HttpMethod.DELETE, "/api/v1/customers/**").hasRole("ADMIN")
                .pathMatchers(HttpMethod.PUT, "/api/v1/customers/**").hasRole("ADMIN")
                .pathMatchers(HttpMethod.POST, "/api/v1/products").hasRole("ADMIN")
                .anyExchange().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return httpSecurity.build();
    }

    /**
     * Keys are fetched from the internal docker hostname, but the iss claim is
     * validated against the external issuer that tokens actually carry. Without
     * this split, every token minted on the host fails validation.
     */
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        return decoder;
    }

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> Flux.fromIterable(realmRoles(jwt)));
        return converter;
    }

    private Collection<GrantedAuthority> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return List.of();
        }
        Object roles = realmAccess.get("roles");
        if (roles instanceof Collection<?> rawRoles) {
            Collection<GrantedAuthority> authorities = rawRoles.stream()
                .map(Object::toString)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
            return authorities;
        }
        return List.of();
    }
}
