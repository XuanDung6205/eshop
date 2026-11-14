package org.example.eshop.commerce.integration;

import org.example.eshop.commerce.common.CommerceException;
import org.example.eshop.commerce.port.InventoryPort.ReservationLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.transaction.IllegalTransactionStateException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="TV3_TEST_DB_URL",matches=".*furniture_shop_test_.*")
class InventoryReservationTest extends CheckoutAdapterFixture {
    @Test void writersRequireAnExistingCallerTransaction() {
        assertThatThrownBy(() -> inventory.reserve(List.of(new ReservationLine(ITEM,VARIANT,1)))).isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> inventory.releaseOrder(ORDER)).isInstanceOf(IllegalTransactionStateException.class);
    }
    @Test void reserveSplitsActiveWarehousesAndOnlyChangesHolds() {
        tx.executeWithoutResult(status -> inventory.reserve(List.of(new ReservationLine(ITEM,VARIANT,7))));
        assertThat(jdbc.queryForList("SELECT quantity FROM inventory_reservations WHERE order_item_id=910001 ORDER BY warehouse_id",Integer.class)).containsExactly(3,4);
        assertThat(jdbc.queryForList("SELECT reserved_quantity FROM inventory_balances WHERE variant_id=910001 ORDER BY warehouse_id",Integer.class)).containsExactly(5,4,0);
        assertThat(jdbc.queryForList("SELECT on_hand_quantity FROM inventory_balances WHERE variant_id=910001 ORDER BY warehouse_id",Integer.class)).containsExactly(5,6,100);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inventory_reservations WHERE order_item_id=910001 AND expires_at IS NOT NULL",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inventory_movements WHERE order_item_id=910001",Integer.class)).isZero();
        assertThat(inventory.availableQuantities(Set.of(VARIANT)).get(VARIANT)).isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT version FROM inventory_balances WHERE variant_id=910001 ORDER BY warehouse_id",Long.class)).containsExactly(1L,1L,0L);
    }
    @Test void insufficientStockAndCallerRollbackLeaveNoPartialHolds() {
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> inventory.reserve(List.of(new ReservationLine(ITEM,VARIANT,10)))))
            .isInstanceOfSatisfying(CommerceException.class,error -> assertThat(error.getCode()).isEqualTo("STOCK_UNAVAILABLE"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inventory_reservations WHERE order_item_id=910001",Integer.class)).isZero();
        tx.executeWithoutResult(status -> {inventory.reserve(List.of(new ReservationLine(ITEM,VARIANT,4)));status.setRollbackOnly();});
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inventory_reservations WHERE order_item_id=910001",Integer.class)).isZero();
        assertThat(inventory.availableQuantities(Set.of(VARIANT)).get(VARIANT)).isEqualTo(9);
    }
    @Test void releaseReturnsOnlyRemainingHoldsAndIsIdempotent() {
        tx.executeWithoutResult(status -> inventory.reserve(List.of(new ReservationLine(ITEM,VARIANT,7))));
        tx.executeWithoutResult(status -> {
            jdbc.update("UPDATE inventory_reservations SET released_quantity=1 WHERE order_item_id=910001 AND warehouse_id=910001");
            jdbc.update("UPDATE inventory_balances SET reserved_quantity=reserved_quantity-1 WHERE variant_id=910001 AND warehouse_id=910001");
        });
        tx.executeWithoutResult(status -> inventory.releaseOrder(ORDER));
        tx.executeWithoutResult(status -> inventory.releaseOrder(ORDER));
        assertThat(jdbc.queryForList("SELECT released_quantity FROM inventory_reservations WHERE order_item_id=910001 ORDER BY warehouse_id",Integer.class)).containsExactly(3,4);
        assertThat(jdbc.queryForList("SELECT reserved_quantity FROM inventory_balances WHERE variant_id=910001 ORDER BY warehouse_id",Integer.class)).containsExactly(2,0,0);
        assertThat(inventory.availableQuantities(Set.of(VARIANT)).get(VARIANT)).isEqualTo(9);
    }
    @Test void fulfilledOrDispatchedOrdersCannotReleaseStock() {
        tx.executeWithoutResult(status -> inventory.reserve(List.of(new ReservationLine(ITEM,VARIANT,3))));
        jdbc.update("UPDATE inventory_reservations SET fulfilled_quantity=1 WHERE order_item_id=910001");
        assertDispatchBlocked();
        jdbc.update("UPDATE inventory_reservations SET fulfilled_quantity=0 WHERE order_item_id=910001");
        jdbc.update("INSERT INTO shipments(order_id,warehouse_id,status,shipped_at) VALUES(910001,910001,'CANCELLED',UTC_TIMESTAMP(6))");
        assertDispatchBlocked();
        assertThat(jdbc.queryForObject("SELECT SUM(released_quantity) FROM inventory_reservations WHERE order_item_id=910001",Integer.class)).isZero();
    }
    private void assertDispatchBlocked() {
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> inventory.releaseOrder(ORDER)))
            .isInstanceOfSatisfying(CommerceException.class,error -> assertThat(error.getCode()).isEqualTo("ORDER_ALREADY_DISPATCHED"));
    }
    @Test void simultaneousOrdersCannotReserveTheSameAvailableUnits() throws Exception {
        var executor=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
        try {
            var first=executor.submit(() -> reserveAfter(start,ITEM));
            var second=executor.submit(() -> reserveAfter(start,ITEM+1));
            start.countDown();
            assertThat(List.of(first.get(20,TimeUnit.SECONDS),second.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder("OK","STOCK_UNAVAILABLE");
        } finally {executor.shutdownNow();}
        assertThat(jdbc.queryForObject("SELECT SUM(quantity) FROM inventory_reservations WHERE order_item_id IN (910001,910002)",Integer.class)).isEqualTo(6);
        assertThat(inventory.availableQuantities(Set.of(VARIANT)).get(VARIANT)).isEqualTo(3);
    }
    private String reserveAfter(CountDownLatch start,long itemId) throws Exception {
        start.await();
        try {tx.executeWithoutResult(status -> inventory.reserve(List.of(new ReservationLine(itemId,VARIANT,6))));return "OK";}
        catch(CommerceException error) {return error.getCode();}
    }
}
