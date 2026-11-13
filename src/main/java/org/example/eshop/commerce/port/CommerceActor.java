package org.example.eshop.commerce.port;
import java.util.Set;
public record CommerceActor(Long userId, byte[] guestHash, Set<String> roles) {
    public CommerceActor { guestHash = guestHash == null ? null : guestHash.clone(); roles = roles == null ? Set.of() : Set.copyOf(roles); }
    @Override public byte[] guestHash() { return guestHash == null ? null : guestHash.clone(); }
}
