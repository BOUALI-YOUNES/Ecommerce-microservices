package com.younes.eccomerc.product;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The product service validates tokens itself rather than relying on the gateway.
 *
 * <p>Browsing the catalogue stays public (matching the gateway rules), but reserving
 * stock and creating products require a token. Previously this service had no security
 * at all, so stock could be reserved by anyone who reached the port directly.
 *
 * <p>The gateway denies the stock release route, but that was never sufficient on its
 * own: the product port is reachable from every container on the docker network, and
 * this service accepted any valid token. An ordinary user token replayed straight at
 * this service inflated a product from 47 to 824 units. The release route is therefore
 * restricted here to the order service's service account (realm role SERVICE), so the
 * rule holds at the service that actually owns the endpoint.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${keycloak.issuer-uri}")
    private String issuerUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                // Must precede the GET rule below and the catch-all: it is the only
                // route a plain user must never be able to reach, directly or through
                // another service on the docker network.
                .requestMatchers(HttpMethod.POST, "/api/v1/products/purchase/release")
                    .hasRole("SERVICE")
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                jwt.jwtAuthenticationConverter(new RealmRoleAuthenticationConverter());
            }));
        return http.build();
    }

    /**
     * Keys are fetched over the internal docker hostname, but the iss claim is
     * validated against the external issuer that tokens actually carry. Without this
     * split, every token minted on the host fails validation.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuerUri));
        return decoder;
    }
}