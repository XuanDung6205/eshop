package org.example.eshop.commerce.promotion;

import org.junit.jupiter.api.Test;
import org.example.eshop.commerce.common.CommerceException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.*;

class CouponCalculatorTest {
    private static final Instant NOW = Instant.parse("2026-11-13T10:00:00Z");
    private final CouponCalculator calculator = new CouponCalculator(Clock.fixed(NOW, ZoneOffset.UTC));

    private CouponEntity coupon(String type, String value) {
        CouponEntity c = new CouponEntity();
        c.setCode("HOME10"); c.setDiscountType(type); c.setDiscountValue(new BigDecimal(value));
        c.setMinimumOrderAmount(BigDecimal.ZERO); c.setIsActive(true);
        c.setStartsAt(LocalDateTime.ofInstant(NOW.minusSeconds(60), ZoneOffset.UTC));
        c.setEndsAt(LocalDateTime.ofInstant(NOW.plusSeconds(60), ZoneOffset.UTC));
        return c;
    }

    @Test void percentageUsesMerchandiseBaseAndRoundsHalfUp() {
        assertEquals(new BigDecimal("15.01"), calculator.discount(coupon("PERCENT", "15"), new BigDecimal("100.05")));
    }
    @Test void capsPercentageAtMaximumDiscount() {
        CouponEntity c=coupon("PERCENT", "20"); c.setMaxDiscountAmount(new BigDecimal("50"));
        assertEquals(new BigDecimal("50.00"),calculator.discount(c,new BigDecimal("1000")));
    }
    @Test void fixedDiscountNeverExceedsMerchandiseBase() {
        assertEquals(new BigDecimal("70.00"),calculator.discount(coupon("FIXED","100"),new BigDecimal("70")));
    }
    @Test void maximumDiscountAlsoAppliesToFixedCoupons() {
        CouponEntity c=coupon("FIXED","100"); c.setMaxDiscountAmount(new BigDecimal("30"));
        assertEquals(new BigDecimal("30.00"),calculator.discount(c,new BigDecimal("200")));
    }
    @Test void startIsInclusiveAndEndIsExclusive() {
        CouponEntity c=coupon("FIXED","10"); c.setStartsAt(LocalDateTime.ofInstant(NOW,ZoneOffset.UTC));
        assertEquals(new BigDecimal("10.00"),calculator.discount(c,new BigDecimal("100")));
        c.setEndsAt(LocalDateTime.ofInstant(NOW,ZoneOffset.UTC));
        assertThrows(CommerceException.class,()->calculator.discount(c,new BigDecimal("100")));
    }
    @Test void rejectsInactiveFutureAndBelowMinimumCoupons() {
        CouponEntity c=coupon("FIXED","10"); c.setIsActive(false);
        assertThrows(CommerceException.class,()->calculator.discount(c,new BigDecimal("100")));
        c.setIsActive(true); c.setStartsAt(LocalDateTime.ofInstant(NOW.plusSeconds(1),ZoneOffset.UTC));
        assertThrows(CommerceException.class,()->calculator.discount(c,new BigDecimal("100")));
        c.setStartsAt(LocalDateTime.ofInstant(NOW.minusSeconds(1),ZoneOffset.UTC)); c.setMinimumOrderAmount(new BigDecimal("200"));
        assertThrows(CommerceException.class,()->calculator.discount(c,new BigDecimal("100")));
    }
    @Test void acceptsZeroBaseWithoutMakingNegativeTotals() {
        assertEquals(new BigDecimal("0.00"),calculator.discount(coupon("FIXED","100"),BigDecimal.ZERO));
        assertThrows(CommerceException.class,()->calculator.discount(coupon("FIXED","100"),new BigDecimal("-1")));
    }
}
