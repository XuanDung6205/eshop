package org.example.eshop.commerce.security;
import java.util.Set;
public record CommerceUserAccount(Long id,String username,String fullName,String passwordHash,String status,Set<String> roles) {
    public CommerceUserAccount { roles=Set.copyOf(roles); }
    public boolean enabled() { return "ACTIVE".equals(status); }
    public CommercePrincipal principal() { return new CommercePrincipal(id,username,fullName,passwordHash,roles); }
}
