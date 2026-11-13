package org.example.eshop.commerce.promotion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="coupons") @Getter @Setter @NoArgsConstructor
public class CouponEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=50) private String code;
    @Column(length=150) private String name;
    @Column(name="discount_type",nullable=false,length=20) private String discountType;
    @Column(name="discount_value",nullable=false,precision=15,scale=2) private BigDecimal discountValue;
    @Column(name="max_discount_amount",precision=15,scale=2) private BigDecimal maxDiscountAmount;
    @Column(name="minimum_order_amount",nullable=false,precision=15,scale=2) private BigDecimal minimumOrderAmount=BigDecimal.ZERO;
    @Column(name="total_usage_limit") private Integer totalUsageLimit;
    @Column(name="per_user_limit") private Integer perUserLimit;
    @Column(name="starts_at",nullable=false) private LocalDateTime startsAt;
    @Column(name="ends_at",nullable=false) private LocalDateTime endsAt;
    @Column(name="is_active",nullable=false) private Boolean isActive=true;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
}
