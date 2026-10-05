package com.younes.order.config;

import java.time.Duration;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Supplies the order service's own access token for internal product callbacks.
 *
 * <p>The stock release endpoint is not reachable by end users, so it cannot be called
 * with the caller's token. Before this existed the release call relayed whatever JWT
 * the shopper happened to hold, which meant the product service had no way to tell a
 * genuine compensation from a user who replayed their own token straight at the
 * product port on the docker network. The product service now requires realm role
 * SERVICE, which only this service account can obtain.
 *
 * <p>The token is cached until shortly before it expires so a rollback does not have to
 * call Keycloak on every attempt, while still refreshing ahead of expiry so an
 * in-flight compensation cannot pick up a token that dies mid-request.
 */
@Component
public class ServiceTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(ServiceTokenProvider.class);

    /**
     * Refresh this long before expiry rather than on expiry, so a token handed out now
     * is still valid when the compensation request actually reaches the product service.
     */
    private static final Duration EXPIRY_MARGIN = Duration.ofSeconds(30);

    private final RestClient restClient;
    private final String tokenUri;
    private final String clientId;
    private final String clientSecret;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public ServiceTokenProvider(
            RestClient.Builder builder,
            @Value("${order.service.token-uri}") String tokenUri,
            @Value("${order.service.client-id}") String clientId,
            @Value("${order.service.client-secret}") String clientSecret) {
        this.restClient = builder.build();
        this.tokenUri = tokenUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * @return a valid service-account bearer token, reusing the cached one while it has
     *         enough life left to survive the call it is used for
     */
    public String getToken() {
        Instant now = Instant.now();
        String current = cachedToken;
        if (current != null && now.plus(EXPIRY_MARGIN).isBefore(expiresAt)) {
            return current;
        }
        synchronized (this) {
            now = Instant.now();
            current = cachedToken;
            if (current != null && now.plus(EXPIRY_MARGIN).isBefore(expiresAt)) {
                return current;
            }
            current = requestToken();
            cachedToken = current;
            return current;
        }
    }

    private String requestToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        TokenResponse response = restClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new IllegalStateException(
                    "Keycloak returned no access token for the order service client '" + clientId + "'");
        }

        long lifespan = response.expiresIn() == null ? 300L : response.expiresIn();
        expiresAt = Instant.now().plusSeconds(lifespan);
        log.debug("Acquired a service token for client {} valid for {}s", clientId, lifespan);
        return response.accessToken();
    }

    /**
     * The claim names are mapped explicitly: Keycloak returns snake_case while the
     * application uses camelCase, and relying on a global naming strategy would leave
     * this silently null if that strategy were ever changed.
     */
    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") Long expiresIn) {
    }
}