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
 * Tự động cập nhật ảnh chính xác 100% khớp với từng sản phẩm trong cơ sở dữ liệu:
 * - Nồi chiên không dầu -> Ảnh nồi chiên
 * - Máy lọc không khí -> Ảnh máy lọc không khí
 * - Robot hút bụi -> Ảnh robot hút bụi
 * - Máy in, chuột, phím, tai nghe, loa, máy ảnh, laptop, tablet, điện thoại...
 */
@Component
@RequiredArgsConstructor
public class ProductDataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ProductDataInitializer.class);
    private final ProductRepository productRepository;

    /** Map ID sản phẩm -> Link ảnh Unsplash khớp chính xác */
    public static final Map<Long, String> EXACT_PRODUCT_IMAGES = new HashMap<>();

    static {
        // 1. Điện thoại & Máy tính bảng
        EXACT_PRODUCT_IMAGES.put(1L, "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=600&auto=format&fit=crop&q=80"); // iPhone 15
        EXACT_PRODUCT_IMAGES.put(2L, "https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?w=600&auto=format&fit=crop&q=80"); // Samsung S24
        EXACT_PRODUCT_IMAGES.put(3L, "https://images.unsplash.com/photo-1598327105666-5b89351aff97?w=600&auto=format&fit=crop&q=80"); // Xiaomi Redmi Note 13
        EXACT_PRODUCT_IMAGES.put(4L, "https://images.unsplash.com/photo-1580910051074-3eb694886505?w=600&auto=format&fit=crop&q=80"); // OPPO Reno11 F
        EXACT_PRODUCT_IMAGES.put(5L, "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=600&auto=format&fit=crop&q=80"); // iPad Air M2
        EXACT_PRODUCT_IMAGES.put(6L, "https://images.unsplash.com/photo-1561154464-82e9adf32764?w=600&auto=format&fit=crop&q=80"); // Galaxy Tab S9 FE
        EXACT_PRODUCT_IMAGES.put(7L, "https://images.unsplash.com/photo-1585790050230-5dd28404ccb9?w=600&auto=format&fit=crop&q=80"); // Xiaomi Pad 6

        // 2. Laptop
        EXACT_PRODUCT_IMAGES.put(8L, "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=600&auto=format&fit=crop&q=80"); // MacBook Air M3
        EXACT_PRODUCT_IMAGES.put(9L, "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=600&auto=format&fit=crop&q=80"); // Dell Inspiron 15
        EXACT_PRODUCT_IMAGES.put(10L, "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=600&auto=format&fit=crop&q=80"); // ASUS Vivobook 15
        EXACT_PRODUCT_IMAGES.put(11L, "https://images.unsplash.com/photo-1541807084-5c52b6b3adef?w=600&auto=format&fit=crop&q=80"); // ThinkPad E14

        // 3. Đồng hồ thông minh
        EXACT_PRODUCT_IMAGES.put(12L, "https://images.unsplash.com/photo-1546868871-7041f2a55e12?w=600&auto=format&fit=crop&q=80"); // Apple Watch SE
        EXACT_PRODUCT_IMAGES.put(13L, "https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?w=600&auto=format&fit=crop&q=80"); // Galaxy Watch6
        EXACT_PRODUCT_IMAGES.put(14L, "https://images.unsplash.com/photo-1575311373937-040b8e1fd5b6?w=600&auto=format&fit=crop&q=80"); // Xiaomi Smart Band 8

        // 4. Tai nghe & Loa
        EXACT_PRODUCT_IMAGES.put(15L, "https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=600&auto=format&fit=crop&q=80"); // AirPods Pro 2
        EXACT_PRODUCT_IMAGES.put(16L, "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=80"); // Sony WH-1000XM5
        EXACT_PRODUCT_IMAGES.put(17L, "https://images.unsplash.com/photo-1484704849700-f032a568e944?w=600&auto=format&fit=crop&q=80"); // JBL Tune 520BT
        EXACT_PRODUCT_IMAGES.put(18L, "https://images.unsplash.com/photo-1590658268037-6bf12165a8df?w=600&auto=format&fit=crop&q=80"); // Soundcore Liberty 4 NC
        EXACT_PRODUCT_IMAGES.put(19L, "https://images.unsplash.com/photo-1545454675-3531b543be5d?w=600&auto=format&fit=crop&q=80"); // JBL Flip 6
        EXACT_PRODUCT_IMAGES.put(20L, "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=600&auto=format&fit=crop&q=80"); // Marshall Emberton II
        EXACT_PRODUCT_IMAGES.put(21L, "https://images.unsplash.com/photo-1589003077984-894e133dabab?w=600&auto=format&fit=crop&q=80"); // Sony SRS-XB100

        // 5. Chuột & Bàn phím
        EXACT_PRODUCT_IMAGES.put(22L, "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600&auto=format&fit=crop&q=80"); // Akko 3087 Dragon Ball
        EXACT_PRODUCT_IMAGES.put(23L, "https://images.unsplash.com/photo-1541140532154-b024d705b909?w=600&auto=format&fit=crop&q=80"); // Logitech K380
        EXACT_PRODUCT_IMAGES.put(24L, "https://images.unsplash.com/photo-1595225476474-87563907a212?w=600&auto=format&fit=crop&q=80"); // Keychron K2 V2
        EXACT_PRODUCT_IMAGES.put(25L, "https://images.unsplash.com/photo-1615663245857-ac93bb7c39e7?w=600&auto=format&fit=crop&q=80"); // Logitech M331 Silent
        EXACT_PRODUCT_IMAGES.put(26L, "https://images.unsplash.com/photo-1626806787461-102c1bfaaea1?w=600&auto=format&fit=crop&q=80"); // Logitech MX Master 3S
        EXACT_PRODUCT_IMAGES.put(27L, "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=600&auto=format&fit=crop&q=80"); // Razer DeathAdder

        // 6. Màn hình
        EXACT_PRODUCT_IMAGES.put(28L, "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=600&auto=format&fit=crop&q=80"); // LG 24MP60G
        EXACT_PRODUCT_IMAGES.put(29L, "https://images.unsplash.com/photo-1547119957-637f8679db1e?w=600&auto=format&fit=crop&q=80"); // Dell UltraSharp U2424H
        EXACT_PRODUCT_IMAGES.put(30L, "https://images.unsplash.com/photo-1585792180666-f7347c490ee2?w=600&auto=format&fit=crop&q=80"); // Samsung Odyssey G5
        EXACT_PRODUCT_IMAGES.put(31L, "https://images.unsplash.com/photo-1551645120-d70bfe84c826?w=600&auto=format&fit=crop&q=80"); // ASUS ProArt

        // 7. Máy ảnh
        EXACT_PRODUCT_IMAGES.put(32L, "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=600&auto=format&fit=crop&q=80"); // Canon EOS R50
        EXACT_PRODUCT_IMAGES.put(33L, "https://images.unsplash.com/photo-1502982720700-bfff97f2ecac?w=600&auto=format&fit=crop&q=80"); // Sony ZV-E10
        EXACT_PRODUCT_IMAGES.put(34L, "https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=600&auto=format&fit=crop&q=80"); // Instax Mini 12

        // 8. Phụ kiện sạc
        EXACT_PRODUCT_IMAGES.put(35L, "https://images.unsplash.com/photo-1583863788434-e58a36330cf0?w=600&auto=format&fit=crop&q=80"); // Anker 20W
        EXACT_PRODUCT_IMAGES.put(36L, "https://images.unsplash.com/photo-1609592424367-9d7a2fa265a7?w=600&auto=format&fit=crop&q=80"); // Pin dự phòng Xiaomi
        EXACT_PRODUCT_IMAGES.put(37L, "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600&auto=format&fit=crop&q=80"); // Cáp Ugreen 100W
        EXACT_PRODUCT_IMAGES.put(38L, "https://images.unsplash.com/photo-1622445262464-84b14e3295b6?w=600&auto=format&fit=crop&q=80"); // Củ sạc Baseus GaN 65W

        // 9. Thiết bị mạng
        EXACT_PRODUCT_IMAGES.put(39L, "https://images.unsplash.com/photo-1544197150-b99a580bb7a8?w=600&auto=format&fit=crop&q=80"); // Router TP-Link Archer AX23
        EXACT_PRODUCT_IMAGES.put(40L, "https://images.unsplash.com/photo-1606904825846-647eb07f5be2?w=600&auto=format&fit=crop&q=80"); // TP-Link Deco X20 Mesh

        // 10. Thiết bị lưu trữ
        EXACT_PRODUCT_IMAGES.put(41L, "https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=600&auto=format&fit=crop&q=80"); // SSD Samsung T7
        EXACT_PRODUCT_IMAGES.put(42L, "https://images.unsplash.com/photo-1628155930542-3c7a64e2c833?w=600&auto=format&fit=crop&q=80"); // USB SanDisk Ultra
        EXACT_PRODUCT_IMAGES.put(43L, "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=600&auto=format&fit=crop&q=80"); // SSD WD Blue SN580
        EXACT_PRODUCT_IMAGES.put(44L, "https://images.unsplash.com/photo-1531492746076-161ca9bcad58?w=600&auto=format&fit=crop&q=80"); // HDD Seagate One Touch

        // 11. Máy in
        EXACT_PRODUCT_IMAGES.put(45L, "https://images.unsplash.com/photo-1612815154858-60aa4c59eaa6?w=600&auto=format&fit=crop&q=80"); // Canon LBP2900
        EXACT_PRODUCT_IMAGES.put(46L, "https://images.unsplash.com/photo-1589330694653-dad6ef49c654?w=600&auto=format&fit=crop&q=80"); // HP LaserJet M211dw
        EXACT_PRODUCT_IMAGES.put(47L, "https://images.unsplash.com/photo-1563245372-f21724e3856d?w=600&auto=format&fit=crop&q=80"); // Epson EcoTank L3250

        // 12. Gia dụng thông minh
        EXACT_PRODUCT_IMAGES.put(48L, "https://images.unsplash.com/photo-1518640467707-6811f4a6ab73?w=600&auto=format&fit=crop&q=80"); // Robot hút bụi Xiaomi E10
        EXACT_PRODUCT_IMAGES.put(49L, "https://images.unsplash.com/photo-1585338107529-13afc5f02586?w=600&auto=format&fit=crop&q=80"); // Nồi chiên không dầu Philips HD9200
        EXACT_PRODUCT_IMAGES.put(50L, "https://images.unsplash.com/photo-1584622650111-993a426fbf0a?w=600&auto=format&fit=crop&q=80"); // Máy lọc không khí Xiaomi 4 Lite
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
                log.info(">>> [eShop] Đã cập nhật ảnh khớp chính xác 100% cho {} sản phẩm trong CSDL MariaDB", updatedCount);
            }
        } catch (Exception e) {
            log.warn("Lưu ý: Bỏ qua tự động cập nhật ảnh sản phẩm: {}", e.getMessage());
        }
    }
}
