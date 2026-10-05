package com.younes.order;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import com.sun.net.httpserver.HttpServer;
import com.younes.order.config.RestTemplateConfig;
import com.younes.order.config.ServiceTokenProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks which bearer token each product-facing RestTemplate presents.
 *
 * <p>The product service accepts the stock release route only from realm role SERVICE.
 * If that call went out with the shopper's token it would be refused and the rollback
 * compensation would never land, leaving reserved stock stranded. If the purchase call
 * went out with the service token instead, the product service could no longer tell the
 * shopper from another service, so both directions are pinned here.
 *
 * <p>A JDK HTTP server stands in for Keycloak and for the product service, which keeps
 * the assertion on the real beans and needs no network, credentials or Eureka.
 */
class RestTemplateServiceIdentityTest {

    private HttpServer server;
    private String baseUrl;
    private final List<String> receivedAuthorizationHeaders = new ArrayList<>();

    private static Jwt shopperJwt() {
        return Jwt.withTokenValue("shopper-token")
                .header("alg", "none")
                .issuedAt(java.time.Instant.EPOCH)
                .expiresAt(java.time.Instant.EPOCH.plusSeconds(3600))
                .subject("shopper")
                .claim("realm_access", java.util.Map.of("roles", List.of("USER")))
                .build();
    }

    @BeforeEach
    void startStubServers() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        // Stands in for the Keycloak token endpoint.
        server.createContext("/token", exchange -> {
            var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (!body.contains("grant_type=client_credentials")) {
                respond(exchange, HttpStatus.BAD_REQUEST, "{\"error\":\"unsupported_grant_type\"}");
                return;
            }
            respond(exchange, HttpStatus.OK,
                    "{\"access_token\":\"service-token\",\"expires_in\":300,\"token_type\":\"Bearer\"}");
        });

        // Stands in for the product service: records who called it.
        server.createContext("/release", exchange -> {
            receivedAuthorizationHeaders.add(
                    String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            respond(exchange, HttpStatus.NO_CONTENT, "");
        });

        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, HttpStatus status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        exchange.sendResponseHeaders(status.value(), bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }

    @AfterEach
    void stopStubServers() {
        server.stop(0);
        SecurityContextHolder.clearContext();
    }

    private ServiceTokenProvider tokenProvider() {
        return new ServiceTokenProvider(
                RestClient.builder(),
                baseUrl + "/token",
                "ecom-order-service",
                "test-secret");
    }

    private RestTemplateConfig config() {
        RestTemplateConfig config = new RestTemplateConfig();
        ReflectionTestUtils.setField(config, "connectTimeout", Duration.ofSeconds(2));
        ReflectionTestUtils.setField(config, "readTimeout", Duration.ofSeconds(5));
        return config;
    }

    private void callRelease(RestTemplate template) {
        template.exchange(baseUrl + "/release", HttpMethod.POST,
                new org.springframework.http.HttpEntity<>("[]"), Void.class);
    }

    @Test
    @DisplayName("the release call presents the service token, never the shopper's")
    void releasePresentsServiceToken() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(shopperJwt()));

        callRelease(config().serviceRestTemplate(new RestTemplateBuilder(), tokenProvider()));

        assertThat(receivedAuthorizationHeaders).containsExactly("Bearer service-token");
    }

    @Test
    @DisplayName("the purchase call presents the shopper's token, as the product service authorizes the user")
    void userCallPresentsShopperToken() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(shopperJwt()));

        callRelease(config().userRestTemplate(new RestTemplateBuilder()));

        assertThat(receivedAuthorizationHeaders).containsExactly("Bearer shopper-token");
    }

    @Test
    @DisplayName("the service token is reused while valid instead of re-requested per call")
    void serviceTokenIsCached() {
        var provider = tokenProvider();
        var template = config().serviceRestTemplate(new RestTemplateBuilder(), provider);

        callRelease(template);
        callRelease(template);

        assertThat(receivedAuthorizationHeaders).containsExactly("Bearer service-token", "Bearer service-token");
    }

    @Test
    @DisplayName("no shopper token is sent when there is no authenticated caller")
    void serviceCallWorksWithoutAShopper() {
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        callRelease(config().serviceRestTemplate(new RestTemplateBuilder(), tokenProvider()));

        assertThat(receivedAuthorizationHeaders).containsExactly("Bearer service-token");
    }
}