package org.example.eshop.config;

import lombok.RequiredArgsConstructor;
import org.example.eshop.entity.Product;
import org.example.eshop.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tự động đồng bộ ảnh đồ trang trí nội thất khớp chính xác với 40 sản phẩm mẫu của DecorHome.
 */
@Component
@RequiredArgsConstructor
public class ProductDataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ProductDataInitializer.class);
    private final ProductRepository productRepository;

    /** Map ID sản phẩm -> Link ảnh nội thất Unsplash khớp chính xác */
    public static final Map<Long, String> EXACT_PRODUCT_IMAGES = new HashMap<>();

    static {
        // 1. Đèn trang trí
        EXACT_PRODUCT_IMAGES.put(1L, "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(2L, "https://images.unsplash.com/photo-1513506003901-1e6a229e2d15?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(3L, "https://images.unsplash.com/photo-1540932239986-30128078f3c5?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(4L, "https://images.unsplash.com/photo-1534105615256-13940a56ff44?w=600&auto=format&fit=crop&q=80");

        // 2. Tranh treo tường
        EXACT_PRODUCT_IMAGES.put(5L, "https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(6L, "https://images.unsplash.com/photo-1582561424760-0321d75e81fa?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(7L, "https://images.unsplash.com/photo-1513519245088-029cb45c9034?w=600&auto=format&fit=crop&q=80");

        // 3. Bình hoa & Gốm sứ
        EXACT_PRODUCT_IMAGES.put(8L, "https://images.unsplash.com/photo-1612196808214-b8e1d6145a8c?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(9L, "https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(10L, "https://images.unsplash.com/photo-1581783898377-1c85bf937427?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(11L, "https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=600&auto=format&fit=crop&q=80");

        // 4. Gương decor
        EXACT_PRODUCT_IMAGES.put(12L, "https://images.unsplash.com/photo-1618220179428-22790b461013?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(13L, "https://images.unsplash.com/photo-1595428774223-ef52624120d2?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(14L, "https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?w=600&auto=format&fit=crop&q=80");

        // 5. Thảm trải sàn
        EXACT_PRODUCT_IMAGES.put(15L, "https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(16L, "https://images.unsplash.com/photo-1600166898405-da9535204843?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(17L, "https://images.unsplash.com/photo-1594040226829-7f251ab46d80?w=600&auto=format&fit=crop&q=80");

        // 6. Gối tựa & Chăn decor
        EXACT_PRODUCT_IMAGES.put(18L, "https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(19L, "https://images.unsplash.com/photo-1540518614846-7eded433c457?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(20L, "https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?w=600&auto=format&fit=crop&q=80");

        // 7. Đồng hồ treo tường
        EXACT_PRODUCT_IMAGES.put(21L, "https://images.unsplash.com/photo-1563861826100-9cb868fdbe1c?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(22L, "https://images.unsplash.com/photo-1509048191080-d2984bad6ae5?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(23L, "https://images.unsplash.com/photo-1533090161767-e6ffed986c88?w=600&auto=format&fit=crop&q=80");

        // 8. Kệ gỗ & Giá treo
        EXACT_PRODUCT_IMAGES.put(24L, "https://images.unsplash.com/photo-1594620302200-9a762244a156?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(25L, "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(26L, "https://images.unsplash.com/photo-1558997519-83ea9252edf8?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(27L, "https://images.unsplash.com/photo-1538688525198-9b88f6f53126?w=600&auto=format&fit=crop&q=80");

        // 9. Nến thơm & Tinh dầu
        EXACT_PRODUCT_IMAGES.put(28L, "https://images.unsplash.com/photo-1602874801007-bd458bb1b8b6?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(29L, "https://images.unsplash.com/photo-1608571423902-eed4a5ad8108?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(30L, "https://images.unsplash.com/photo-1572726729207-a78d6feb18d7?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(31L, "https://images.unsplash.com/photo-1603006905003-be475563bc59?w=600&auto=format&fit=crop&q=80");

        // 10. Cây giả trang trí
        EXACT_PRODUCT_IMAGES.put(32L, "https://images.unsplash.com/photo-1463320726281-696a485928c7?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(33L, "https://images.unsplash.com/photo-1485955900006-10f4d324d411?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(34L, "https://images.unsplash.com/photo-1459411552884-841db9b3cc2a?w=600&auto=format&fit=crop&q=80");

        // 11. Tượng & Đồ trưng bày
        EXACT_PRODUCT_IMAGES.put(35L, "https://images.unsplash.com/photo-1513519245088-029cb45c9034?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(36L, "https://images.unsplash.com/photo-1616486029423-aaa4789e8c9a?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(37L, "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=600&auto=format&fit=crop&q=80");

        // 12. Ghế thư giãn & Đôn sofa
        EXACT_PRODUCT_IMAGES.put(38L, "https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(39L, "https://images.unsplash.com/photo-1598300042247-d088f8ab3a91?w=600&auto=format&fit=crop&q=80");
        EXACT_PRODUCT_IMAGES.put(40L, "https://images.unsplash.com/photo-1580480055273-228ff5388ef8?w=600&auto=format&fit=crop&q=80");
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            List<Product> products = productRepository.findAll();
            int updatedCount = 0;

            for (Product product : products) {
                String exactUrl = EXACT_PRODUCT_IMAGES.get(product.getId());
                if (exactUrl != null && !exactUrl.equals(product.getImageUrl())) {
                    product.setImageUrl(exactUrl);
                    updatedCount++;
                }
            }

            if (updatedCount > 0) {
                productRepository.saveAll(products);
                log.info(">>> [DecorHome] Đã cập nhật ảnh nội thất khớp chính xác cho {} sản phẩm trong CSDL", updatedCount);
            }
        } catch (Exception e) {
            log.warn("Lưu ý: Bỏ qua tự động cập nhật ảnh sản phẩm: {}", e.getMessage());
        }
    }
}
