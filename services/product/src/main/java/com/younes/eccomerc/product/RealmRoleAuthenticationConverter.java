package com.younes.eccomerc.product;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Maps realm roles from the token into Spring authorities, so route rules can be
 * expressed in terms of roles rather than scopes.
 *
 * <p>The stock release route is restricted to realm role SERVICE, which only the order
 * service's service account holds. Keycloak puts those roles in the realm_access claim,
 * not in scope, so the default scope-only mapping leaves every caller without the role.
 *
 * <p>Kept as a separate type rather than a private method so the mapping can be
 * asserted directly: a converter that silently drops the claim would make the release
 * rule unreachable and the service unable to compensate at all.
 */
public class RealmRoleAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final Converter<Jwt, Collection<GrantedAuthority>> scopes =
            new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(source -> {
            // Retained so authenticated() behaves as before for ordinary routes.
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(source));
            authorities.addAll(realmRoles(source));
            return authorities;
        });
        return converter.convert(jwt);
    }

    private Collection<GrantedAuthority> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return List.of();
        }
        Object roles = realmAccess.get("roles");
        if (roles instanceof Collection<?> rawRoles) {
            return rawRoles.stream()
                    .map(Object::toString)
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
        }
        return List.of();
    }
}