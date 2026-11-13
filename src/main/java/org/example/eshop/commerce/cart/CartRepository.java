package org.example.eshop.commerce.cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface CartRepository extends JpaRepository<CartEntity,Long> {
    Optional<CartEntity> findByActiveUserId(Long userId);
    Optional<CartEntity> findByActiveGuestHash(byte[] hash);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from CartEntity c where c.id=:id")
    Optional<CartEntity> lockById(@Param("id")Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from CartEntity c where c.id in :ids order by c.id")
    List<CartEntity> lockInIdOrder(@Param("ids")Collection<Long> ids);
}
