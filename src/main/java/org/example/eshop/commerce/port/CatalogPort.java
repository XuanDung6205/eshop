package org.example.eshop.commerce.port;
import java.util.Map;
import java.util.Set;
public interface CatalogPort { Map<Long,VariantSnapshot> findVariants(Set<Long> ids); }
