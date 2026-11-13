package org.example.eshop.commerce.promotion;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
public record CouponView(Long id,String code,String name,String discountType,String discountValue,
    String maxDiscountAmount,String minimumOrderAmount,Integer totalUsageLimit,Integer perUserLimit,
    Instant startsAt,Instant endsAt,Boolean isActive,Instant createdAt,Instant updatedAt) {
    static String money(BigDecimal value){return value==null?null:value.setScale(2).toPlainString();}
    static Instant utc(LocalDateTime value){return value==null?null:value.toInstant(ZoneOffset.UTC);}
    static CouponView from(CouponEntity c){return new CouponView(c.getId(),c.getCode(),c.getName(),c.getDiscountType(),money(c.getDiscountValue()),money(c.getMaxDiscountAmount()),money(c.getMinimumOrderAmount()),c.getTotalUsageLimit(),c.getPerUserLimit(),utc(c.getStartsAt()),utc(c.getEndsAt()),c.getIsActive(),utc(c.getCreatedAt()),utc(c.getUpdatedAt()));}
}
