package org.example.eshop.commerce.integration;

import org.example.eshop.commerce.security.CommerceUserAccount;
import org.example.eshop.commerce.security.UserDirectory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;

@Repository
@Transactional(readOnly=true)
public class JdbcUserDirectory implements UserDirectory {
    private final NamedParameterJdbcTemplate jdbc;
    public JdbcUserDirectory(NamedParameterJdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public Optional<CommerceUserAccount> findByUsername(String username) {
        var rows=jdbc.query("SELECT id,username,full_name,password_hash,status FROM users WHERE username=:username",Map.of("username",username),
            (rs,row) -> new CommerceUserAccount(rs.getLong("id"),rs.getString("username"),rs.getString("full_name"),rs.getString("password_hash"),rs.getString("status"),Set.of()));
        return rows.stream().findFirst().map(this::withRoles);
    }
    @Override public Optional<CommerceUserAccount> findById(Long id) {
        var rows=jdbc.query("SELECT id,username,full_name,status FROM users WHERE id=:id",Map.of("id",id),
            (rs,row) -> new CommerceUserAccount(rs.getLong("id"),rs.getString("username"),rs.getString("full_name"),null,rs.getString("status"),Set.of()));
        return rows.stream().findFirst().map(this::withRoles);
    }
    private CommerceUserAccount withRoles(CommerceUserAccount account) {
        var roles=jdbc.queryForList("SELECT r.code FROM roles r JOIN user_roles ur ON ur.role_id=r.id WHERE ur.user_id=:id",Map.of("id",account.id()),String.class);
        var allowed=new LinkedHashSet<String>();
        for(String role:roles) if(Set.of("ADMIN","STAFF","CUSTOMER").contains(role)) allowed.add(role);
        return new CommerceUserAccount(account.id(),account.username(),account.fullName(),account.passwordHash(),account.status(),allowed);
    }
}
