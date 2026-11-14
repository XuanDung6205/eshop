package org.example.eshop.commerce.port;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface InventoryPort {
    Map<Long,Integer> availableQuantities(Set<Long> ids);
    record ReservationLine(long orderItemId,long variantId,int quantity) {}
    /** Joins the caller's writable transaction; the caller must hold the order root lock. */
    void reserve(List<ReservationLine> lines);
    /** Idempotently releases unfulfilled holds in the caller's writable transaction. */
    void releaseOrder(long orderId);
}
