package org.example.eshop.commerce.integration;

import org.example.eshop.commerce.port.InventoryPort;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;

@Repository
@Transactional(readOnly=true)
public class JdbcInventoryAdapter implements InventoryPort {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcInventoryAdapter(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public Map<Long,Integer> availableQuantities(Set<Long> ids) {
        if(ids.isEmpty()) return Map.of();
        var result=new LinkedHashMap<Long,Integer>();
        ids.forEach(id -> result.put(id,0));
        jdbc.query("""
            SELECT b.variant_id,SUM(b.on_hand_quantity-b.reserved_quantity) AS available
            FROM inventory_balances b JOIN warehouses w ON w.id=b.warehouse_id
            WHERE w.is_active=1 AND b.variant_id IN (:ids) GROUP BY b.variant_id
            """,Map.of("ids",ids),rs -> {
                result.put(rs.getLong("variant_id"),(int)Math.min(Integer.MAX_VALUE,Math.max(0L,rs.getLong("available"))));
            });
        return result;
    }
}
