package org.example.eshop.commerce.port;
public interface CurrentActorPort {
    CommerceActor currentActor();
    void clearGuestIdentity();
}
