package com.younes.eccomerc;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.younes.eccomerc.category.Category;
import com.younes.eccomerc.category.CategoryRepo;
import com.younes.eccomerc.product.Product;
import com.younes.eccomerc.product.ProductRepo;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins the authorization rule on the stock release endpoint, against the real security
 * chain and a real database.
 *
 * <p>The gateway already denies this route, but the product port is reachable from
 * every container on the docker network, so the rule has to hold at this service too.
 * Before it did, a replayed user token inflated a product from 47 to 824 units.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductReleaseAuthorizationTest {

    private static final String RELEASE_URL = "/api/v1/products/purchase/release";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private CategoryRepo categoryRepo;

    private Integer productId;

    @BeforeEach
    void setUp() {
        productRepo.deleteAll();
        categoryRepo.deleteAll();

        var category = categoryRepo.save(
                Category.builder().name("Laptops").description("Laptops").build());
        productId = productRepo.save(Product.builder()
                .name("Test product")
                .description("Test product")
                .availableQuantity(10)
                .price(new java.math.BigDecimal("100.00"))
                .category(category)
                .build())
                .getId();
    }

    /**
     * Authorities are attached the way {@link com.younes.eccomerc.product.RealmRoleAuthenticationConverter}
     * would map them. MockMvc's jwt() helper builds authorities from scopes alone, so
     * setting realm_access here would have no effect on the request and the allow case
     * would pass or fail for the wrong reason. The claim mapping itself is asserted
     * separately in RealmRoleAuthenticationConverterTest.
     */
    private static RequestPostProcessor tokenWithRoles(String... roles) {
        return jwt().authorities(Arrays.stream(roles)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toArray(GrantedAuthority[]::new));
    }

    private double remainingStock() {
        return productRepo.findById(productId).orElseThrow().getAvailableQuantity();
    }

    private org.springframework.test.web.servlet.ResultActions releaseWith(RequestPostProcessor token, String quantity)
            throws Exception {
        return mockMvc.perform(post(RELEASE_URL)
                .with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("[{\"productId\":" + productId + ",\"quantity\":" + quantity + "}]"));
    }

    @Test
    @DisplayName("a plain user token cannot release stock")
    void userTokenIsRejected() throws Exception {
        releaseWith(tokenWithRoles("USER"), "1000").andExpect(status().isForbidden());

        // Stock must be untouched, not merely reported as an error afterwards.
        org.assertj.core.api.Assertions.assertThat(remainingStock()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("an admin token still cannot release stock, the route is service-only")
    void adminTokenIsRejected() throws Exception {
        releaseWith(tokenWithRoles("USER", "ADMIN"), "1000").andExpect(status().isForbidden());

        org.assertj.core.api.Assertions.assertThat(remainingStock()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("an anonymous caller is rejected")
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(post(RELEASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"productId\":" + productId + ",\"quantity\":1000}]"))
                .andExpect(status().isUnauthorized());

        org.assertj.core.api.Assertions.assertThat(remainingStock()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("the order service account can release stock")
    void serviceAccountIsAllowed() throws Exception {
        releaseWith(tokenWithRoles("SERVICE"), "4").andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(remainingStock()).isEqualTo(14.0);
    }
}