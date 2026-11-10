-- Script cập nhật ảnh sản phẩm mẫu chất lượng cao cho eShop
-- Chạy script này trên MariaDB nếu sản phẩm trong DB chưa có ảnh hoặc ảnh bị lỗi

-- 1. Chuột máy tính (Mouse)
UPDATE product
SET image_url = 'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=600&auto=format&fit=crop&q=80'
WHERE (image_url IS NULL OR image_url = '' OR image_url LIKE '%placeholder%')
  AND (LOWER(name) LIKE '%chuột%' OR LOWER(name) LIKE '%mouse%');

-- 2. Bàn phím (Keyboard)
UPDATE product
SET image_url = 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600&auto=format&fit=crop&q=80'
WHERE (image_url IS NULL OR image_url = '' OR image_url LIKE '%placeholder%')
  AND (LOWER(name) LIKE '%phím%' OR LOWER(name) LIKE '%keyboard%');

-- 3. Tai nghe (Headphones / Earphones)
UPDATE product
SET image_url = 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600&auto=format&fit=crop&q=80'
WHERE (image_url IS NULL OR image_url = '' OR image_url LIKE '%placeholder%')
  AND (LOWER(name) LIKE '%tai nghe%' OR LOWER(name) LIKE '%headphone%' OR LOWER(name) LIKE '%earphone%');

-- 4. Laptop / Máy tính xách tay
UPDATE product
SET image_url = 'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=600&auto=format&fit=crop&q=80'
WHERE (image_url IS NULL OR image_url = '' OR image_url LIKE '%placeholder%')
  AND (LOWER(name) LIKE '%laptop%' OR LOWER(name) LIKE '%macbook%' OR LOWER(name) LIKE '%thinkpad%');

-- 5. Màn hình (Monitor / Display)
UPDATE product
SET image_url = 'https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=600&auto=format&fit=crop&q=80'
WHERE (image_url IS NULL OR image_url = '' OR image_url LIKE '%placeholder%')
  AND (LOWER(name) LIKE '%màn hình%' OR LOWER(name) LIKE '%monitor%');

-- 6. Điện thoại (Phone / Smartphone)
UPDATE product
SET image_url = 'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=600&auto=format&fit=crop&q=80'
WHERE (image_url IS NULL OR image_url = '' OR image_url LIKE '%placeholder%')
  AND (LOWER(name) LIKE '%điện thoại%' OR LOWER(name) LIKE '%iphone%' OR LOWER(name) LIKE '%samsung%' OR LOWER(name) LIKE '%xiaomi%');

-- 7. Đồng hồ thông minh (Smartwatch)
UPDATE product
SET image_url = 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600&auto=format&fit=crop&q=80'
WHERE (image_url IS NULL OR image_url = '' OR image_url LIKE '%placeholder%')
  AND (LOWER(name) LIKE '%đồng hồ%' OR LOWER(name) LIKE '%watch%');

-- 8. Các sản phẩm còn lại chưa có ảnh -> Ảnh công nghệ tổng hợp
UPDATE product
SET image_url = 'https://images.unsplash.com/photo-1526738549149-8e07eca6c147?w=600&auto=format&fit=crop&q=80'
WHERE image_url IS NULL OR image_url = '';
