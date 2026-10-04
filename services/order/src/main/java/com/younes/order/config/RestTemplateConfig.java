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
 */
@Configuration
public class RestTemplateConfig {

    @Value("${application.config.connect-timeout:2s}")
    private Duration connectTimeout;

    @Value("${application.config.read-timeout:10s}")
    private Duration readTimeout;

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        var settings = HttpClientSettings.defaults().withTimeouts(connectTimeout, readTimeout);
        // Boot 4 accepts either a factory Class or a Supplier here; there is no
        // overload taking a pre-built factory instance, so it is passed as a Supplier.
        ClientHttpRequestFactory requestFactory =
                ClientHttpRequestFactoryBuilder.detect().build(settings);

        return builder
                .requestFactory(() -> requestFactory)
                .additionalInterceptors(bearerTokenInterceptor())
                .build();
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