package com.younes.order.models;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Releases stock that was reserved for an order whose transaction did not complete.
 *
 * <p>The stock reservation happens in the product service, which is a different
 * database and cannot join this transaction. Without this, any failure after the
 * reservation left stock permanently consumed with no corresponding order. This
 * registers an after-rollback callback so the reservation is undone exactly when the
 * local transaction does not commit.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCompensation {

    private final ProductClient productClient;

    /**
     * Registers a release of {@code reservedLines} to run if the current transaction
     * rolls back. A no-op when no transaction is active.
     */
    public void releaseOnRollback(List<PurchaseRequest> reservedLines) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("No active transaction, cannot schedule a stock release for {}", reservedLines);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    release(reservedLines);
                }
            }
        });
    }

    private void release(List<PurchaseRequest> lines) {
        try {
            productClient.releaseStock(lines);
            log.info("Released stock for a rolled back order: {}", lines);
        } catch (RuntimeException e) {
            // The order is already gone; failing here would only mask the original
            // error, so record it loudly for reconciliation instead.
            log.error("Could not release stock after a rolled back order, manual reconciliation "
                    + "is required for lines {}: {}", lines, e.getMessage());
        }
    }
}