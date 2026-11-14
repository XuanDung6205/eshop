package org.example.eshop.commerce.port;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/** Current TV2 catalog data used to calculate and persist checkout snapshots. */
public interface CheckoutCatalogPort {
    record Product(long variantId,long productId,String sku,String productName,String variantName,String imageUrl,String colorName,String dimensionsSnapshot,String materialsSnapshot,int warrantyMonths,boolean assemblyRequired,BigDecimal unitPrice,boolean active) {}
    Map<Long,Product> findProducts(Set<Long> ids);
}
