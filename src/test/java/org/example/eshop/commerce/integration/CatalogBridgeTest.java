package org.example.eshop.commerce.integration;

import org.junit.jupiter.api.Test;
import org.example.eshop.commerce.common.CommerceException;
import org.example.eshop.commerce.port.InventoryPort;
import org.example.eshop.commerce.port.VariantSnapshot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogBridgeTest {
    @Test void priceIsDecimalStringAndAvailabilityDefaultsToZero() {
        var catalog = mock(JdbcCatalogAdapter.class);
        InventoryPort inventory = mock(InventoryPort.class);
        when(inventory.availableQuantities(anySet())).thenReturn(Map.of(10L,7));
        when(catalog.search("table",0,12)).thenReturn(new JdbcCatalogAdapter.VariantPage(List.of(
            new VariantSnapshot(10L,1L,"TABLE-10","Table","Oak",new BigDecimal("1250000.00"),"/oak.jpg",true),
            new VariantSnapshot(11L,1L,"TABLE-11","Table","Walnut",new BigDecimal("1300000.00"),null,true)),2));
        var page = new CatalogBridgeController(catalog,inventory).variants("table",0,12);
        assertThat(page.items()).hasSize(2);
        assertThat(page.items().get(0).price()).isEqualTo("1250000.00");
        assertThat(page.items().get(0).availableQuantity()).isEqualTo(7);
        assertThat(page.items().get(1).availableQuantity()).isZero();
        assertThat(page.totalPages()).isEqualTo(1);
    }
    @Test void invalidPageBoundsFailBeforeDatabaseAccess() {
        var controller = new CatalogBridgeController(mock(JdbcCatalogAdapter.class), mock(InventoryPort.class));
        assertThatThrownBy(() -> controller.variants("",-1,12)).isInstanceOf(CommerceException.class);
        assertThatThrownBy(() -> controller.variants("",0,0)).isInstanceOf(CommerceException.class);
        assertThatThrownBy(() -> controller.variants("",0,101)).isInstanceOf(CommerceException.class);
    }
}
