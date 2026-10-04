package com.younes.ecommerce.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.younes.ecommerce.CustomerRequest;
import com.younes.ecommerce.CustomerResponse;
import com.younes.ecommerce.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Registers the profile for the authenticated caller.
     *
     * <p>This used to be permitAll and accepted a client-supplied id, so an anonymous
     * caller could overwrite any existing customer document. It now requires a token
     * and binds the profile to the caller's identity.
     */
    @PostMapping
    public ResponseEntity<String> createCustomer(
            @RequestBody @Valid CustomerRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(customerService.createCustomer(request, jwt.getSubject()));
    }

    /**
     * Updates the caller's own profile, so no client-supplied identifier is involved.
     */
    @PutMapping("/me")
    public ResponseEntity<Void> editCustomer(
            @RequestBody @Valid CustomerRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        customerService.editCustomer(request, jwt.getSubject());
        return ResponseEntity.ok().build();
    }

    /**
     * The caller's own profile.
     */
    @GetMapping("/me")
    public ResponseEntity<CustomerResponse> currentCustomer(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(customerService.findByKeycloakId(jwt.getSubject()));
    }

    /**
     * Listing every customer exposes every profile, so it is ADMIN only.
     *
     * <p>Enforced by the gateway and repeated in the service, because this service is
     * reachable from every other container on the compose network.
     */
    @GetMapping
    public ResponseEntity<List<CustomerResponse>> customers(Authentication authentication) {
        requireAdmin(authentication);
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @GetMapping("/exists/{customerId}")
    public ResponseEntity<Boolean> customerExistsById(
            @PathVariable("customerId") String customerId,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                customerService.existsById(customerId, jwt.getSubject(), isAdmin(authentication)));
    }

    /**
     * A caller may read their own profile; reading somebody else's requires ADMIN.
     * Without this check any authenticated user could read another customer's name,
     * email and address by id.
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> findCustomerById(
            @PathVariable("customerId") String customerId,
            @AuthenticationPrincipal Jwt jwt,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                customerService.findById(customerId, jwt.getSubject(), isAdmin(authentication)));
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteCustomer(
            @PathVariable("customerId") String customerId,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        customerService.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private void requireAdmin(Authentication authentication) {
        if (!isAdmin(authentication)) {
            throw new AccessDeniedException("This operation requires the ADMIN role");
        }
    }

}