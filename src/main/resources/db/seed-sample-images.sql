-- Script cập nhật ảnh sản phẩm nội thất mẫu chất lượng cao
-- Chạy script này trên MariaDB nếu sản phẩm trong DB chưa có ảnh hoặc ảnh bị lỗi

-- 1. Đèn trang trí
UPDATE product
SET image_url = 'https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=600&auto=format&fit=crop&q=80'
WHERE (image_url IS NULL OR image_url = '' OR image_url LIKE '%placeholder%')
  AND LOWER(name) LIKE '%đèn%';

-- 2. Tranh treo tường
UPDATE product
SET image_url = 'https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=600&auto=format&fit=crop&q=80'
WHERE (image_url IS NULL OR image_url = '' OR image_url LIKE '%placeholder%')
  AND LOWER(name) LIKE '%tranh%';

-- 3. Bình hoa & Gốm sứ
UPDATE product
SET image_url = 'https://images.unsplash.com/photo-1612196808214-b8e1d6145a8c?w=600&auto=format&fit=crop&q=80'
WHERE (image_url IS NULL OR image_url = '' OR image_url LIKE '%placeholder%')
  AND (LOWER(name) LIKE '%bình%' OR LOWER(name) LIKE '%lọ%' OR LOWER(name) LIKE '%gốm%');

-- 4. Gương decor
UPDATE product
SET image_url = 'https://images.unsplash.com/photo-1618220179428-22790b461013?w=600&auto=format&fit=crop&q=80'
WHERE (image_url IS NULL OR image_url = '' OR image_url LIKE '%placeholder%')
  AND LOWER(name) LIKE '%gương%';

-- 5. Thảm trải sàn
UPDATE product
SET image_url = 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?w=600&auto=format&fit=crop&q=80'
WHERE (image_url IS NULL OR image_url = '' OR image_url LIKE '%placeholder%')
  AND LOWER(name) LIKE '%thảm%';

-- 6. Gối tựa & Chăn decor
UPDATE product
SET image_url = 'https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?w=600&auto=format&fit=crop&q=80'
WHERE (image_url IS NULL OR image_url = '' OR image_url LIKE '%placeholder%')
  AND (LOWER(name) LIKE '%gối%' OR LOWER(name) LIKE '%chăn%');

-- 7. Cây giả trang trí
UPDATE product
SET image_url = 'https://images.unsplash.com/photo-1463320726281-696a485928c7?w=600&auto=format&fit=crop&q=80'
WHERE (image_url IS NULL OR image_url = '' OR image_url LIKE '%placeholder%')
  AND (LOWER(name) LIKE '%cây%' OR LOWER(name) LIKE '%chậu%');

-- 8. Các sản phẩm nội thất còn lại chưa có ảnh
UPDATE product
SET image_url = 'https://images.unsplash.com/photo-1513694203232-719a280e022f?w=600&auto=format&fit=crop&q=80'
WHERE image_url IS NULL OR image_url = '';
