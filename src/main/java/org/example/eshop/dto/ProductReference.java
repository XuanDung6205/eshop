package org.example.eshop.dto;

import java.math.BigDecimal;

/** Current catalogue data used to ground a product answer. */
public record ProductReference(Long id,
                               String name,
                               String description,
                               String categoryName,
                               BigDecimal price,
                               Integer quantity,
                               String url,
                               Double similarityScore) {
}
