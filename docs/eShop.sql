-- =====================================================================
--  CSDL mẫu cho dự án Website bán đồ trang trí nội thất DecorHome
--  MariaDB 11.7+      Charset: utf8mb4
--
--  Tài khoản mẫu (mật khẩu đều là 123456, đã băm BCrypt):
--     seller1   / 123456  -> ROLE_SELLER
--     seller2   / 123456  -> ROLE_SELLER
--     customer1 / 123456  -> ROLE_CUSTOMER
--     customer2 / 123456  -> ROLE_CUSTOMER
--     locked    / 123456  -> ROLE_CUSTOMER (bị khoá, enabled = 0)
--  "Khách" (GUEST) = người chưa đăng nhập, KHÔNG có dòng trong bảng users.
-- =====================================================================

DROP DATABASE IF EXISTS eshop;
CREATE DATABASE eshop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE eshop;

-- ---------------------------------------------------------------------
-- A. DANH MỤC & SẢN PHẨM TRANG TRÍ NỘI THẤT
-- ---------------------------------------------------------------------
CREATE TABLE category (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT pk_category PRIMARY KEY (id),
    CONSTRAINT uq_category_name UNIQUE (name)
) ENGINE = InnoDB;

CREATE TABLE product (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    name        VARCHAR(150)  NOT NULL,
    description VARCHAR(500),
    price       DECIMAL(15,2) NOT NULL,
    quantity    INT           NOT NULL DEFAULT 0,
    image_url   VARCHAR(500),
    category_id BIGINT        NOT NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_product PRIMARY KEY (id),
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT ck_product_price CHECK (price >= 0),
    CONSTRAINT ck_product_quantity CHECK (quantity >= 0)
) ENGINE = InnoDB;

CREATE INDEX idx_product_name ON product (name);

INSERT INTO category (id, name, description) VALUES
 (1,  'Đèn trang trí',          'Đèn bàn ngủ, đèn cây đứng phòng khách, đèn thả trần ánh sáng ấm'),
 (2,  'Tranh treo tường',       'Tranh canvas tối giản, tranh trừu tượng, bộ tranh nghệ thuật Bắc Âu'),
 (3,  'Bình hoa & Gốm sứ',      'Bình gốm Bát Tràng, lọ hoa sứ mờ phong cách Japandi, bình thủy tinh'),
 (4,  'Gương decor',            'Gương đứng toàn thân khung gỗ sồi, gương tròn treo tường, gương lượn sóng'),
 (5,  'Thảm trải sàn',          'Thảm phòng khách dệt sợi đay, thảm lông cừu nhân tạo, thảm họa tiết tối giản'),
 (6,  'Gối tựa & Chăn decor',   'Gối tựa sofa vải linen, gối nhung sang trọng, chăn phủ sofa dệt kim'),
 (7,  'Đồng hồ treo tường',     'Đồng hồ gỗ tự nhiên kim trôi êm ái, đồng hồ kim loại tối giản'),
 (8,  'Kệ gỗ & Giá treo',       'Kệ sách gỗ sồi đứng, kệ gỗ treo tường trang trí, giá treo đồ đa năng'),
 (9,  'Nến thơm & Tinh dầu',    'Nến thơm sáp đậu nành thiên nhiên, tinh dầu khuếch tán que mây'),
 (10, 'Cây giả trang trí',      'Cây xanh nội thất không cần chăm sóc, cây bàng Singapore, cây oliu Bắc Âu'),
 (11, 'Tượng & Đồ trưng bày',   'Tượng nghệ thuật trừu tượng, khay đá marble, đồ decor bàn làm việc'),
 (12, 'Ghế thư giãn & Đôn sofa','Ghế bành đọc sách bọc vải nỉ ấm áp, đôn sofa chân gỗ sồi, ghế mây đan');

INSERT INTO product (name, description, price, quantity, image_url, category_id) VALUES
-- 1. Đèn trang trí (1-4)
 ('Đèn bàn gốm sứ chao vải Linen',     'Ánh sáng vàng ấm 3000K dịu mắt, chân đế gốm thủ công mộc mạc cho phòng ngủ',               690000, 25, 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=600&auto=format&fit=crop&q=80', 1),
 ('Đèn cây đứng phòng khách Bắc Âu',   'Thân hợp kim sơn tĩnh điện kết hợp gỗ sồi, cao 1m65, phù hợp đặt cạnh sofa đọc sách',     1490000, 14, 'https://images.unsplash.com/photo-1513506003901-1e6a229e2d15?w=600&auto=format&fit=crop&q=80', 1),
 ('Đèn thả trần mây tre đan thủ công', 'Chất liệu mây tự nhiên đan tay tạo hiệu ứng bóng nắng ấm áp cho bàn ăn và ban công',       850000, 20, 'https://images.unsplash.com/photo-1540932239986-30128078f3c5?w=600&auto=format&fit=crop&q=80', 1),
 ('Đèn ngủ nấm thủy tinh Retro',       'Thiết kế dáng nấm cổ điển tối giản, điều chỉnh 3 mức độ sáng chạm cảm ứng tiện lợi',        540000, 32, 'https://images.unsplash.com/photo-1534105615256-13940a56ff44?w=600&auto=format&fit=crop&q=80', 1),

-- 2. Tranh treo tường (5-7)
 ('Bộ 3 tranh Canvas tối giản Wabi Sabi','Khung gỗ thông tự nhiên kèm vải canvas in mực UV bền màu, kích thước 40x60cm mỗi bức',     790000, 18, 'https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=600&auto=format&fit=crop&q=80', 2),
 ('Tranh trừu tượng khối nổi Beige',   'Tông màu be kem và nâu đất nhẹ nhàng, phù hợp treo mảng tường lớn sau ghế sofa phòng khách',  1150000, 12, 'https://images.unsplash.com/photo-1582561424760-0321d75e81fa?w=600&auto=format&fit=crop&q=80', 2),
 ('Tranh lá cây nhiệt đới khung gỗ sồi','Họa tiết lá xanh rêu tươi mát, giúp không gian phòng làm việc thêm thư giãn',                450000, 28, 'https://images.unsplash.com/photo-1513519245088-029cb45c9034?w=600&auto=format&fit=crop&q=80', 2),

-- 3. Bình hoa & Gốm sứ (8-11)
 ('Bình gốm sứ nhám dáng Donut Japandi','Gốm nung nhiệt độ cao bề mặt sần mộc màu trắng kem, cắm cành khô hoặc hoa lau tuyệt đẹp',    380000, 40, 'https://images.unsplash.com/photo-1612196808214-b8e1d6145a8c?w=600&auto=format&fit=crop&q=80', 3),
 ('Lọ hoa gốm Bát Tràng men hỏa biến', 'Men gốm thủ công sắc xanh rêu chuyển nâu trầm độc bản, cao 28cm sang trọng',                  590000, 22, 'https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?w=600&auto=format&fit=crop&q=80', 3),
 ('Bình hoa thủy tinh gân sọc hổ phách','Thủy tinh dày dặn trong suốt tông màu nâu hổ phách cổ điển cho bàn ăn hoặc kệ sách',        290000, 35, 'https://images.unsplash.com/photo-1581783898377-1c85bf937427?w=600&auto=format&fit=crop&q=80', 3),
 ('Set 3 bình gốm mini trang trí kệ',  'Bộ 3 lọ gốm kích thước nhỏ 10-18cm tông màu đất nung ấm áp, dễ phối trên kệ gỗ',            420000, 30, 'https://images.unsplash.com/photo-1610701596007-11502861dcfa?w=600&auto=format&fit=crop&q=80', 3),

-- 4. Gương decor (12-14)
 ('Gương đứng toàn thân khung gỗ sồi', 'Kích thước 60x165cm, kính tráng bạc chống ố nét cao, có chân đứng gập gọn tiện di chuyển',   1890000, 10, 'https://images.unsplash.com/photo-1618220179428-22790b461013?w=600&auto=format&fit=crop&q=80', 4),
 ('Gương tròn treo tường viền da nâu', 'Đường kính 60cm kèm dây da treo tường phong cách Scandinavian cho phòng tắm hoặc bàn phấn',   650000, 20, 'https://images.unsplash.com/photo-1595428774223-ef52624120d2?w=600&auto=format&fit=crop&q=80', 4),
 ('Gương lượn sóng viền bọc nỉ kem',   'Kiểu dáng uốn lượn nghệ thuật trẻ trung, kích thước 50x150cm tạo điểm nhấn góc check-in',   1550000, 12, 'https://images.unsplash.com/photo-1616486338812-3dadae4b4ace?w=600&auto=format&fit=crop&q=80', 4),

-- 5. Thảm trải sàn (15-17)
 ('Thảm dệt sợi đay tự nhiên hình tròn','Đường kính 1m5 đan sợi cói tự nhiên thân thiện môi trường, mang hơi thở nhiệt đới mộc mạc', 780000, 16, 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=600&auto=format&fit=crop&q=80', 5),
 ('Thảm phòng khách lông cừu Bắc Âu',  'Kích thước 1m6x2m3 mềm mịn êm chân, họa tiết kẻ hình học kem nâu chống trượt mặt đáy',       1390000, 15, 'https://images.unsplash.com/photo-1600166898405-da9535204843?w=600&auto=format&fit=crop&q=80', 5),
 ('Thảm chùi chân dệt họa tiết Vintage','Sợi cotton dệt dày thấm hút tốt, kích thước 50x80cm phù hợp cửa ra vào hoặc cạnh giường ngủ', 240000, 45, 'https://images.unsplash.com/photo-1594040226829-7f251ab46d80?w=600&auto=format&fit=crop&q=80', 5),

-- 6. Gối tựa & Chăn decor (18-20)
 ('Gối tựa lưng sofa vải Linen màu rêu','Kích thước 45x45cm kèm ruột bông gòn 3D đàn hồi cao, vỏ vải đũi thoáng mát tháo giặt dễ',     220000, 50, 'https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?w=600&auto=format&fit=crop&q=80', 6),
 ('Set 2 gối tựa nhung gân nâu Caramel','Chất liệu nhung tăm sang trọng tạo chiều sâu ấm cúng cho ghế sofa phòng khách',              410000, 30, 'https://images.unsplash.com/photo-1540518614846-7eded433c457?w=600&auto=format&fit=crop&q=80', 6),
 ('Chăn phủ sofa dệt kim tua rua',      'Kích thước 130x180cm bằng len cotton mềm nhẹ, vừa trang trí ghế bành vừa đắp giữ ấm',      490000, 24, 'https://images.unsplash.com/photo-1522771739844-6a9f6d5f14af?w=600&auto=format&fit=crop&q=80', 6),

-- 7. Đồng hồ treo tường (21-23)
 ('Đồng hồ treo tường gỗ sồi kim trôi', 'Mặt gỗ nguyên khối đường kính 35cm, máy Quartz chạy êm tuyệt đối không gây tiếng tích tắc',   580000, 22, 'https://images.unsplash.com/photo-1563861826100-9cb868fdbe1c?w=600&auto=format&fit=crop&q=80', 7),
 ('Đồng hồ nghệ thuật khung sắt tối giản','Đường kính 50cm sơn tĩnh điện đen nhám, thiết kế kim đồng thanh mảnh cho phòng khách hiện đại',720000, 14, 'https://images.unsplash.com/photo-1509048191080-d2984bad6ae5?w=600&auto=format&fit=crop&q=80', 7),
 ('Đồng hồ để bàn gỗ óc chó Vintage',   'Kích thước nhỏ gọn 15x15cm có đèn nền dịu nhẹ xem giờ ban đêm, phù hợp tab đầu giường',     360000, 25, 'https://images.unsplash.com/photo-1533090161767-e6ffed986c88?w=600&auto=format&fit=crop&q=80', 7),

-- 8. Kệ gỗ & Giá treo (24-27)
 ('Kệ sách đứng 4 tầng gỗ sồi tự nhiên','Kích thước 60x30x145cm chịu lực tốt, phủ dầu lau bóng giữ nguyên vân gỗ ấm áp',            1790000, 12, 'https://images.unsplash.com/photo-1594620302200-9a762244a156?w=600&auto=format&fit=crop&q=80', 8),
 ('Thanh kệ gỗ treo tường trang trí',   'Gỗ thông nhập khẩu dài 80cm kèm pat sắt ẩn, phù hợp bày bình gốm nhỏ, nến thơm và tranh ảnh', 290000, 40, 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=600&auto=format&fit=crop&q=80', 8),
 ('Giá treo quần áo chữ A gỗ sồi',      'Thiết kế khung chữ A kèm kệ để giày bên dưới, giúp phòng ngủ tối giản gọn gàng hơn',         950000, 15, 'https://images.unsplash.com/photo-1558997519-83ea9252edf8?w=600&auto=format&fit=crop&q=80', 8),
 ('Kệ góc 3 tầng tre tự nhiên',         'Tận dụng góc tường trống để đặt chậu cây, sách hoặc đồ trang trí nhỏ xinh',                460000, 20, 'https://images.unsplash.com/photo-1538688525198-9b88f6f53126?w=600&auto=format&fit=crop&q=80', 8),

-- 9. Nến thơm & Tinh dầu (28-31)
 ('Nến thơm sáp đậu nành hương Gỗ Thông','Hũ thủy tinh nâu hổ phách 200g đốt 45 giờ, mùi hương rừng thông và tuyết tùng thư giãn',     260000, 60, 'https://images.unsplash.com/photo-1602874801007-bd458bb1b8b6?w=600&auto=format&fit=crop&q=80', 9),
 ('Bộ khuếch tán tinh dầu que mây Trà Trắng','Dung tích 120ml tỏa hương nhẹ nhàng suốt 2 tháng, kèm lọ thủy tinh tối giản sang trọng',  320000, 48, 'https://images.unsplash.com/photo-1608571423902-eed4a5ad8108?w=600&auto=format&fit=crop&q=80', 9),
 ('Đèn đốt nến thơm đá Marble điều chỉnh sáng','Làm tan chảy sáp không cần lửa an toàn tuyệt đối, vừa làm đèn ngủ vừa tỏa hương thơm',690000, 18, 'https://images.unsplash.com/photo-1572726729207-a78d6feb18d7?w=600&auto=format&fit=crop&q=80', 9),
 ('Nến thơm hương Vani & Hổ Phách ấm áp','Hũ gốm thủ công nắp gỗ sồi 250g, tạo bầu không khí lãng mạn ấm cúng cho buổi tối',          350000, 35, 'https://images.unsplash.com/photo-1603006905003-be475563bc59?w=600&auto=format&fit=crop&q=80', 9),

-- 10. Cây giả trang trí (32-34)
 ('Cây Bàng Singapore giả cao 1m2 kèm chậu mây','Lá nhựa PE gân nổi giống thật đến 95%, mang sắc xanh sức sống cho góc phòng khách',   690000, 20, 'https://images.unsplash.com/photo-1463320726281-696a485928c7?w=600&auto=format&fit=crop&q=80', 10),
 ('Cây Oliu Bắc Âu decor cao 1m5',      'Dáng cây thanh mảnh sang trọng kèm chậu gốm trắng tối giản, lý tưởng cho căn hộ studio',       920000, 14, 'https://images.unsplash.com/photo-1485955900006-10f4d324d411?w=600&auto=format&fit=crop&q=80', 10),
 ('Chậu cây Monstera để bàn làm việc',  'Cao 45cm kèm chậu xi măng đá mài nhỏ gọn, dễ đặt trên bàn học, kệ tivi hoặc quầy lễ tân',   280000, 38, 'https://images.unsplash.com/photo-1459411552884-841db9b3cc2a?w=600&auto=format&fit=crop&q=80', 10),

-- 11. Tượng & Đồ trưng bày (35-37)
 ('Tượng trừu tượng gốm sứ Đôi Tay',    'Chất liệu gốm nung trắng nhám cao 24cm, tạo điểm nhấn nghệ thuật tinh tế trên bàn trà',      390000, 26, 'https://images.unsplash.com/photo-1513519245088-029cb45c9034?w=600&auto=format&fit=crop&q=80', 11),
 ('Khay đựng trang sức đá Marble vân mây','Đá tự nhiên viền ánh kim dùng bày nước hoa, đồng hồ, nến thơm trên bàn trang điểm',        310000, 32, 'https://images.unsplash.com/photo-1616486029423-aaa4789e8c9a?w=600&auto=format&fit=crop&q=80', 11),
 ('Cặp tượng Hươu vàng đồng sang trọng','Chất liệu composite mạ đồng bền màu, biểu tượng bình an và tài lộc cho kệ phòng khách',     520000, 18, 'https://images.unsplash.com/photo-1513694203232-719a280e022f?w=600&auto=format&fit=crop&q=80', 11),

-- 12. Ghế thư giãn & Đôn sofa (38-40)
 ('Ghế bành thư giãn bọc nỉ kem ấm áp', 'Đệm mút D40 êm ái kết hợp khung gỗ tần bì chắc chắn, thiết kế ôm lưng đọc sách cực thoải mái',3290000, 8,  'https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?w=600&auto=format&fit=crop&q=80', 12),
 ('Đôn sofa tròn bọc vải nhung xanh rêu','Đường kính 40cm chân gỗ sồi tự nhiên, dùng gác chân hoặc làm ghế ngồi phụ xinh xắn',       680000, 18, 'https://images.unsplash.com/photo-1598300042247-d088f8ab3a91?w=600&auto=format&fit=crop&q=80', 12),
 ('Ghế mây Papasan thư giãn kèm nệm dày','Khung mây song tự nhiên uốn cong đường kính 1m kèm nệm bông dày êm ái cho góc ban công',  2490000, 10, 'https://images.unsplash.com/photo-1580480055273-228ff5388ef8?w=600&auto=format&fit=crop&q=80', 12);

-- ---------------------------------------------------------------------
-- B. TÀI KHOẢN (Spring Security)
-- role: ROLE_SELLER | ROLE_CUSTOMER   (ROLE_GUEST gán cho người chưa đăng nhập)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(100) NOT NULL,                 -- BCrypt 60 ký tự
    full_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(100) NOT NULL,
    phone      VARCHAR(20),
    address    VARCHAR(255),
    role       VARCHAR(30)  NOT NULL DEFAULT 'ROLE_CUSTOMER',
    enabled    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('ROLE_SELLER', 'ROLE_CUSTOMER'))
) ENGINE = InnoDB;

-- Mật khẩu: 123456
INSERT INTO users (username, password, full_name, email, phone, address, role, enabled) VALUES
 ('seller1',   '$2a$10$uPUNNrC1JUnzBYDZD6AQBe.swyBhdnARmWl.Ctpo737mZZF4hejyS', 'Trần Văn Quản Trị', 'seller1@decorhome.vn', '0901000001', '12 Nguyễn Văn Bảo, Gò Vấp, TP.HCM', 'ROLE_SELLER',   TRUE),
 ('seller2',   '$2a$10$uPUNNrC1JUnzBYDZD6AQBe.swyBhdnARmWl.Ctpo737mZZF4hejyS', 'Lê Thị Decor',      'seller2@decorhome.vn', '0901000002', '45 Lê Lợi, Quận 1, TP.HCM',         'ROLE_SELLER',   TRUE),
 ('customer1', '$2a$10$uPUNNrC1JUnzBYDZD6AQBe.swyBhdnARmWl.Ctpo737mZZF4hejyS', 'Nguyễn Văn An',     'customer1@gmail.com',  '0912000001', '78 Phan Văn Trị, Bình Thạnh, TP.HCM','ROLE_CUSTOMER', TRUE),
 ('customer2', '$2a$10$uPUNNrC1JUnzBYDZD6AQBe.swyBhdnARmWl.Ctpo737mZZF4hejyS', 'Phạm Thị Mai',      'customer2@gmail.com',  '0912000002', '9 Võ Văn Ngân, Thủ Đức, TP.HCM',    'ROLE_CUSTOMER', TRUE),
 ('locked',    '$2a$10$uPUNNrC1JUnzBYDZD6AQBe.swyBhdnARmWl.Ctpo737mZZF4hejyS', 'Tài Khoản Khoá',    'locked@gmail.com',     '0912000009', 'TP.HCM',                            'ROLE_CUSTOMER', FALSE);

-- ---------------------------------------------------------------------
-- C. ĐƠN HÀNG (Giỏ hàng + Thanh toán)
-- ---------------------------------------------------------------------
CREATE TABLE orders (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    order_date     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    customer_name  VARCHAR(100)  NOT NULL,
    phone          VARCHAR(20)   NOT NULL,
    address        VARCHAR(255)  NOT NULL,
    note           VARCHAR(500),
    payment_method VARCHAR(20)   NOT NULL,            -- COD | BANKING
    status         VARCHAR(20)   NOT NULL DEFAULT 'NEW', -- NEW | PAID | SHIPPING | DONE | CANCELLED
    total_amount   DECIMAL(15,2) NOT NULL,
    user_id        BIGINT        NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE = InnoDB;

CREATE TABLE order_detail (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    order_id   BIGINT        NOT NULL,
    product_id BIGINT        NOT NULL,
    quantity   INT           NOT NULL,
    unit_price DECIMAL(15,2) NOT NULL,
    CONSTRAINT pk_order_detail PRIMARY KEY (id),
    CONSTRAINT fk_od_order   FOREIGN KEY (order_id)   REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_od_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT ck_od_quantity CHECK (quantity > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- KIỂM TRA
-- ---------------------------------------------------------------------
SELECT 'category' AS tbl, COUNT(*) AS total FROM category
UNION ALL SELECT 'product', COUNT(*) FROM product
UNION ALL SELECT 'users',   COUNT(*) FROM users;
-- Kết quả mong đợi: category 12, product 40, users 5
