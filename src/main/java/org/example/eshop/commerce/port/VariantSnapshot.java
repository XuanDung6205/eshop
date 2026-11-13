package org.example.eshop.commerce.port;
import java.math.BigDecimal;
public record VariantSnapshot(Long id, Long productId, String sku, String productName, String variantName, BigDecimal price, String imageUrl, boolean active) {}
