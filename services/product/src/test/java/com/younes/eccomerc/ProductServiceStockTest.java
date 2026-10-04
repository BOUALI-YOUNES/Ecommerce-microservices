package com.younes.eccomerc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.younes.eccomerc.category.Category;
import com.younes.eccomerc.category.CategoryRepo;
import com.younes.eccomerc.product.Product;
import com.younes.eccomerc.product.ProductPurchasRequest;
import com.younes.eccomerc.product.ProductRepo;
import com.younes.eccomerc.product.ProductService;
import com.younes.eccomerc.Exception.ProductPurchasException;

import jakarta.persistence.EntityNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the stock reservation behaviour of {@link ProductService}.
 *
 * Uses a real (in-memory) database and real transactions, so the conditional UPDATE
 * and the rollback semantics are genuinely exercised.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProductServiceStockTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private CategoryRepo categoryRepo;

    private Integer categoryId;

    @BeforeEach
    void setUp() {
        productRepo.deleteAll();
        categoryRepo.deleteAll();

        var category = categoryRepo.save(
                Category.builder().name("Laptops").description("Laptops").build());
        categoryId = category.getId();
    }

    private Integer givenProductWithStock(double stock) {
        return productRepo.save(Product.builder()
                .name("Test product")
                .description("Test product")
                .availableQuantity(stock)
                .price(new java.math.BigDecimal("100.00"))
                .category(categoryRepo.findById(categoryId).orElseThrow())
                .build())
                .getId();
    }

    private static ProductPurchasRequest purchase(Integer productId, double quantity) {
        var request = new ProductPurchasRequest();
        setQuantity(request, quantity);
        setProductId(request, productId);
        return request;
    }

    private static void setQuantity(ProductPurchasRequest target, double quantity) {
        try {
            var field = ProductPurchasRequest.class.getDeclaredField("quantity");
            field.setAccessible(true);
            field.set(target, quantity);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setProductId(ProductPurchasRequest target, Integer productId) {
        try {
            var field = ProductPurchasRequest.class.getDeclaredField("productId");
            field.setAccessible(true);
            field.set(target, productId);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private double remainingStock(Integer productId) {
        return productRepo.findById(productId)
                .map(Product::getAvailableQuantity)
                .orElseThrow(() -> new EntityNotFoundException("missing product"));
    }

    @Test
    @DisplayName("a successful purchase decrements stock exactly once")
    void successfulPurchaseDecrementsStock() {
        Integer productId = givenProductWithStock(10);

        var response = productService.purchasProducts(List.of(purchase(productId, 3)));

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getQuantity()).isEqualTo(3);
        assertThat(remainingStock(productId)).isEqualTo(7.0);
    }

    @Test
    @DisplayName("insufficient stock is rejected and leaves stock untouched")
    void insufficientStockIsRejected() {
        Integer productId = givenProductWithStock(2);

        assertThatThrownBy(() -> productService.purchasProducts(List.of(purchase(productId, 5))))
                .isInstanceOf(ProductPurchasException.class);

        assertThat(remainingStock(productId)).isEqualTo(2.0);
    }

    @Test
    @DisplayName("a failing line rolls back the decrement of the earlier lines")
    void failingLineRollsBackEarlierLines() {
        Integer firstProduct = givenProductWithStock(10);
        Integer secondProduct = givenProductWithStock(1);

        assertThatThrownBy(() -> productService.purchasProducts(List.of(
                purchase(firstProduct, 2),
                purchase(secondProduct, 5))))
                .isInstanceOf(ProductPurchasException.class);

        // The first line must not stay consumed: the whole purchase is rejected.
        assertThat(remainingStock(firstProduct)).isEqualTo(10.0);
        assertThat(remainingStock(secondProduct)).isEqualTo(1.0);
    }

    @Test
    @DisplayName("zero and negative quantities are rejected")
    void nonPositiveQuantitiesAreRejected() {
        Integer productId = givenProductWithStock(10);

        assertThatThrownBy(() -> productService.purchasProducts(List.of(purchase(productId, 0))))
                .isInstanceOf(ProductPurchasException.class);
        assertThatThrownBy(() -> productService.purchasProducts(List.of(purchase(productId, -5))))
                .isInstanceOf(ProductPurchasException.class);

        assertThat(remainingStock(productId)).isEqualTo(10.0);
    }

    @Test
    @DisplayName("duplicate product ids in one request are summed, not rejected")
    void duplicateProductIdsAreSummed() {
        Integer productId = givenProductWithStock(10);

        var response = productService.purchasProducts(List.of(
                purchase(productId, 2),
                purchase(productId, 3)));

        assertThat(response).hasSize(1);
        assertThat(remainingStock(productId)).isEqualTo(5.0);
    }

    @Test
    @DisplayName("concurrent purchases of the last unit cannot oversell")
    void concurrentPurchasesCannotOversell() throws Exception {
        Integer productId = givenProductWithStock(1);
        int threads = 8;

        var barrier = new CyclicBarrier(threads);
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<Boolean>> tasks = IntStream.range(0, threads)
                    .<Callable<Boolean>>mapToObj(i -> () -> {
                        barrier.await(10, TimeUnit.SECONDS);
                        try {
                            productService.purchasProducts(List.of(purchase(productId, 1)));
                            return true;
                        } catch (RuntimeException e) {
                            return false;
                        }
                    })
                    .toList();

            List<Future<Boolean>> results = new ArrayList<>();
            for (Callable<Boolean> task : tasks) {
                results.add(executor.submit(task));
            }

            int succeeded = 0;
            for (Future<Boolean> result : results) {
                if (result.get(30, TimeUnit.SECONDS)) {
                    succeeded++;
                }
            }

            // Only one thread can legitimately buy the single remaining unit.
            assertThat(succeeded).isEqualTo(1);
            assertThat(remainingStock(productId)).isEqualTo(0.0);
        } finally {
            executor.shutdownNow();
        }
    }
}