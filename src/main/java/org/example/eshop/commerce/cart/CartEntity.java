package org.example.eshop.commerce.cart;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name="carts") @Getter @Setter @NoArgsConstructor
public class CartEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="user_id") private Long userId;
    @Column(name="guest_token_hash",columnDefinition="BINARY(32)") private byte[] guestTokenHash;
    @Column(nullable=false,length=20) private String status="ACTIVE";
    @Column(name="expires_at") private LocalDateTime expiresAt;
    @Column(name="active_user_id",insertable=false,updatable=false) private Long activeUserId;
    @Column(name="active_guest_hash",columnDefinition="BINARY(32)",insertable=false,updatable=false) private byte[] activeGuestHash;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
}
