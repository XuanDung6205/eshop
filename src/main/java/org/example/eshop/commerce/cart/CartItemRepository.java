package org.example.eshop.commerce.cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface CartItemRepository extends JpaRepository<CartItemEntity,Long> {
    List<CartItemEntity> findByCartIdOrderById(Long cartId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CartItemEntity> findByCartIdAndVariantId(Long cartId,Long variantId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from CartItemEntity i where i.cartId=:cartId order by i.id")
    List<CartItemEntity> lockedCartItems(@Param("cartId")Long cartId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from CartItemEntity i where i.id=:id and i.cartId=:cartId")
    Optional<CartItemEntity> lockOwnedItem(@Param("id")Long id,@Param("cartId")Long cartId);
    @Modifying(flushAutomatically=true) @Query("delete from CartItemEntity i where i.cartId=:cartId")
    void deleteCartItems(@Param("cartId")Long cartId);
}
