package org.example.eshop.commerce.security;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.io.Serial;
import java.util.Collection;
import java.util.Set;

public final class CommercePrincipal implements UserDetails, CredentialsContainer {
    @Serial private static final long serialVersionUID = 1L;
    private final Long id;
    private final String username;
    private final String fullName;
    private final Set<String> roles;
    private String password;
    public CommercePrincipal(Long id,String username,String fullName,String password,Set<String> roles) {
        this.id=id; this.username=username; this.fullName=fullName; this.password=password; this.roles=Set.copyOf(roles);
    }
    public Long id() { return id; }
    public Long getUserId() { return id; }
    public String fullName() { return fullName; }
    public Set<String> roles() { return roles; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return roles.stream().sorted().map(role -> new SimpleGrantedAuthority("ROLE_"+role)).toList(); }
    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public void eraseCredentials() { password=null; }
}
