package org.example.eshop.commerce.integration;

import org.example.eshop.commerce.port.CheckoutAddressPort;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@Transactional(readOnly=true)
public class JdbcCheckoutAddressAdapter implements CheckoutAddressPort {
    private static final String SELECT="""
        SELECT a.id,a.label,a.recipient_name,a.phone,u.email,a.address_line1,a.address_line2,
               a.ward_name,a.district_name,a.province_name,a.postal_code,a.country_code
        FROM user_addresses a JOIN users u ON u.id=a.user_id
        """;
    private static final RowMapper<Address> MAPPER=(rs,row) -> new Address(rs.getLong("id"),rs.getString("label"),rs.getString("recipient_name"),rs.getString("phone"),rs.getString("email"),rs.getString("address_line1"),rs.getString("address_line2"),rs.getString("ward_name"),rs.getString("district_name"),rs.getString("province_name"),rs.getString("postal_code"),rs.getString("country_code"),null,null);
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcCheckoutAddressAdapter(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public List<Address> findByUserId(long userId) {
        return jdbc.query(SELECT+" WHERE a.user_id=:userId ORDER BY a.is_default DESC,a.id",Map.of("userId",userId),MAPPER);
    }
    @Override public Optional<Address> findOwned(long userId,long addressId) {
        return jdbc.query(SELECT+" WHERE a.user_id=:userId AND a.id=:id",Map.of("userId",userId,"id",addressId),MAPPER).stream().findFirst();
    }
}
