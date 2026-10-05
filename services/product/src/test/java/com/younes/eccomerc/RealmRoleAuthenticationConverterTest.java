package com.younes.eccomerc;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import com.younes.eccomerc.product.RealmRoleAuthenticationConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts the realm_access claim really becomes a Spring authority.
 *
 * <p>The stock release rule is written as hasRole("SERVICE"). If this mapping silently
 * dropped the claim, every caller would be refused and compensations would never land,
 * which is the failure mode that is easy to miss because the security fix would still
 * look correct.
 */
class RealmRoleAuthenticationConverterTest {

    private final RealmRoleAuthenticationConverter converter = new RealmRoleAuthenticationConverter();

    private static Jwt tokenWith(Map<String, Object> realmAccess) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "none")
                .issuedAt(Instant.EPOCH)
                .expiresAt(Instant.EPOCH.plusSeconds(3600))
                .subject("someone");
        if (realmAccess != null) {
            builder.claim("realm_access", realmAccess);
        }
        return builder.build();
    }

    private Collection<String> authoritiesOf(Jwt jwt) {
        return converter.convert(jwt).getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    @Test
    @DisplayName("the SERVICE realm role becomes ROLE_SERVICE")
    void serviceRoleIsMapped() {
        Jwt jwt = tokenWith(Map.of("roles", List.of("default-roles-ecom-realm", "SERVICE")));

        assertThat(authoritiesOf(jwt)).contains("ROLE_SERVICE");
    }

    @Test
    @DisplayName("USER and ADMIN are mapped, so gateway parity is kept")
    void userAndAdminRolesAreMapped() {
        Jwt jwt = tokenWith(Map.of("roles", List.of("USER", "ADMIN")));

        assertThat(authoritiesOf(jwt)).contains("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("a token without the realm_access claim yields no role authorities")
    void missingClaimYieldsNoRoles() {
        assertThat(authoritiesOf(tokenWith(null)))
                .noneMatch(authority -> authority.startsWith("ROLE_"));
    }

    @Test
    @DisplayName("a malformed realm_access claim is ignored rather than failing the request")
    void malformedClaimIsIgnored() {
        assertThat(authoritiesOf(tokenWith(Map.of("roles", "SERVICE"))))
                .noneMatch(authority -> authority.equals("ROLE_SERVICE"));
    }
}