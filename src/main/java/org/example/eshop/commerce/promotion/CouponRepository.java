package org.example.eshop.commerce.promotion;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface CouponRepository extends JpaRepository<CouponEntity,Long> {
    @Query("select c from CouponEntity c where :q='' or lower(c.code) like concat('%',:q,'%') or lower(c.name) like concat('%',:q,'%')")
    Page<CouponEntity> search(@Param("q") String query, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from CouponEntity c where c.id=:id")
    Optional<CouponEntity> lockById(@Param("id") Long id);
}
