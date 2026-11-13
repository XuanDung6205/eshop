package org.example.eshop.commerce.security;
import java.util.Optional;
public interface UserDirectory {
    Optional<CommerceUserAccount> findByUsername(String username);
    Optional<CommerceUserAccount> findById(Long id);
}
