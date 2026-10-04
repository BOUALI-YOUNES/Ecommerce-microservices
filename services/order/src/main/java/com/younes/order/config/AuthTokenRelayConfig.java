package com.younes.order.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Forwards the caller's bearer token on outbound service-to-service calls.
 *
 * Without this, every internal call reached the downstream service with no
 * Authorization header and was rejected. Note the downstream services must actually
 * validate the token, which is why they are resource servers themselves.
 */
@Component
public class AuthTokenRelayConfig implements RequestInterceptor {

    static final String AUTHORIZATION = "Authorization";

    @Override
    public void apply(RequestTemplate template) {
        String token = currentBearerToken();
        if (token != null) {
            template.header(AUTHORIZATION, token);
        }
    }

    private String currentBearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return "Bearer " + jwtAuthenticationToken.getToken().getTokenValue();
        }
        return null;
    }
}