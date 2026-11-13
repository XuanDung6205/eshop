package org.example.eshop.commerce.cart;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity @Table(name="cart_items") @Getter @Setter @NoArgsConstructor
public class CartItemEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="cart_id",nullable=false) private Long cartId;
    @Column(name="variant_id",nullable=false) private Long variantId;
    @Column(nullable=false) private Integer quantity;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
}
