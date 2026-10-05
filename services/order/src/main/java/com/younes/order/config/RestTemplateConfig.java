package com.younes.order.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate used for the product purchase call.
 *
 * <p>The previous bean was built with builder.build(), which applied no connect or
 * read timeout at all, so one slow product instance pinned the calling thread
 * indefinitely. Budgets are now explicit.
 *
 * <p>@LoadBalanced lets the call resolve through Eureka, and the interceptor forwards
 * the caller's bearer token so the product service can authenticate the caller.
 *
 * <p>Two instances exist because the product service now treats the stock release
 * endpoint as internal: it accepts only the order service's own account, not a
 * shopper's token. Forwarding the caller's token on that call would be rejected, and
 * attempting the release as the shopper would defeat the restriction.
 */
@Configuration
public class RestTemplateConfig {

    @Value("${application.config.connect-timeout:2s}")
    private Duration connectTimeout;

    @Value("${application.config.read-timeout:10s}")
    private Duration readTimeout;

    /**
     * Used for calls that act on behalf of the end user, such as reserving stock for
     * their order. The product service authorizes those as the shopper.
     */
    @Bean("userRestTemplate")
    @LoadBalanced
    public RestTemplate userRestTemplate(RestTemplateBuilder builder) {
        return builder
                .requestFactory(() -> requestFactory())
                .additionalInterceptors(bearerTokenInterceptor())
                .build();
    }

    /**
     * Used for calls the order service makes as itself, such as releasing stock after a
     * rollback. The caller's token is deliberately not forwarded here: the product
     * service restricts the release endpoint to this service's own account, so
     * relaying a shopper's token would be rejected anyway.
     */
    @Bean("serviceRestTemplate")
    @LoadBalanced
    public RestTemplate serviceRestTemplate(RestTemplateBuilder builder, ServiceTokenProvider tokenProvider) {
        return builder
                .requestFactory(() -> requestFactory())
                .additionalInterceptors((request, body, execution) -> {
                    request.getHeaders().setBearerAuth(tokenProvider.getToken());
                    return execution.execute(request, body);
                })
                .build();
    }

    private ClientHttpRequestFactory requestFactory() {
        var settings = HttpClientSettings.defaults().withTimeouts(connectTimeout, readTimeout);
        // Boot 4 accepts either a factory Class or a Supplier here; there is no
        // overload taking a pre-built factory instance, so it is passed as a Supplier.
        return ClientHttpRequestFactoryBuilder.detect().build(settings);
    }

    private ClientHttpRequestInterceptor bearerTokenInterceptor() {
        return (request, body, execution) -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
                request.getHeaders().setBearerAuth(jwtAuthenticationToken.getToken().getTokenValue());
            }
            return execution.execute(request, body);
        };
    }
}