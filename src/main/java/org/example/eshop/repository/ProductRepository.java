package org.example.eshop.repository;

import org.example.eshop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByCategoryId(Long categoryId);

    @Query("SELECT DISTINCT c.id FROM Category c WHERE LOWER(:query) LIKE CONCAT('%', LOWER(c.name), '%')")
    List<Long> findCategoryIdsMentionedInQuery(@Param("query") String query);

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.quantity > 0 ORDER BY p.id DESC")
    List<Product> findInStockByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.id <> :productId ORDER BY p.id DESC")
    List<Product> findRelatedProducts(@Param("categoryId") Long categoryId,
                                      @Param("productId") Long productId,
                                      Pageable pageable);

    /** Tìm theo tên, mô tả hoặc tên danh mục (không phân biệt hoa thường) và/hoặc theo danh mục; tham số null = bỏ qua điều kiện. */
    @Query("""
        SELECT p FROM Product p
        WHERE (:keyword IS NULL 
            OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR (p.description IS NOT NULL AND LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (p.category IS NOT NULL AND LOWER(p.category.name) LIKE LOWER(CONCAT('%', :keyword, '%'))))
        AND (:categoryId IS NULL OR p.category.id = :categoryId)
    """)
    Page<Product> search(@Param("keyword") String keyword,
                         @Param("categoryId") Long categoryId,
                         Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        WHERE (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR (p.description IS NOT NULL AND LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            OR (p.category IS NOT NULL AND LOWER(p.category.name) LIKE LOWER(CONCAT('%', :keyword, '%'))))
        AND (:categoryId IS NULL OR p.category.id = :categoryId)
        ORDER BY p.id DESC
    """)
    List<Product> findKeywordMatches(@Param("keyword") String keyword,
                                     @Param("categoryId") Long categoryId);
}

