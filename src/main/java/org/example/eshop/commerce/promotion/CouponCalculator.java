package org.example.eshop.commerce.promotion;
import org.example.eshop.commerce.common.CommerceException;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class CouponCalculator {
    private final Clock clock;
    public CouponCalculator(Clock clock){this.clock=clock;}
    public BigDecimal discount(CouponEntity coupon,BigDecimal merchandiseBase) {
        if(merchandiseBase==null || merchandiseBase.signum()<0) throw CommerceException.badRequest("INVALID_AMOUNT","Tiền hàng không được âm.");
        LocalDateTime now=LocalDateTime.ofInstant(clock.instant(),ZoneOffset.UTC);
        if(!Boolean.TRUE.equals(coupon.getIsActive()) || now.isBefore(coupon.getStartsAt()) || !now.isBefore(coupon.getEndsAt()))
            throw CommerceException.conflict("COUPON_UNAVAILABLE","Mã giảm giá đã tắt, chưa đến hạn hoặc đã hết hạn.");
        if(merchandiseBase.compareTo(coupon.getMinimumOrderAmount())<0)
            throw CommerceException.conflict("COUPON_MINIMUM_NOT_MET","Tiền hàng chưa đạt mức tối thiểu của mã giảm giá.");
        BigDecimal discount="PERCENT".equals(coupon.getDiscountType())
                ?merchandiseBase.multiply(coupon.getDiscountValue()).divide(new BigDecimal("100"))
                :coupon.getDiscountValue();
        if(coupon.getMaxDiscountAmount()!=null) discount=discount.min(coupon.getMaxDiscountAmount());
        return discount.min(merchandiseBase).setScale(2,RoundingMode.HALF_UP);
    }
}
