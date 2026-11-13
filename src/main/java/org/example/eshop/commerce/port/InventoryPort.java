package org.example.eshop.commerce.port;
import java.util.Map;
import java.util.Set;
public interface InventoryPort { Map<Long,Integer> availableQuantities(Set<Long> ids); }
