# Chạy TV3-01/02/03 — React + Spring Boot + MariaDB

## Phần đã có

- REST contract và lỗi JSON; session login/logout, CSRF, phân quyền STAFF/ADMIN, token giỏ khách HttpOnly lưu hash trong DB.
- Giỏ theo SKU lưu DB: xem/thêm/sửa/xóa/xóa hết, gộp sau đăng nhập, cảnh báo ngừng bán/hết hàng và giữ giỏ sau refresh. Giỏ không giữ tồn kho.
- Quản trị coupon: phân trang/tìm kiếm/tạo/sửa một phần/bật tắt/xóa mã chưa dùng, xung đột cho mã đã dùng; tính thử phần trăm/số tiền/cap/minimum/thời hạn.
- Giao diện React cho khách và quản trị, desktop/mobile, loading/rỗng/lỗi, hiển thị số tiền do server tính.
- Checkout, tạo đơn, thu/hoàn tiền là TV3-04 trở đi, chưa triển khai ở đây. Tính thử coupon không giữ lượt và chưa kiểm tra hạn mức redemption đồng thời.

## Cấu trúc và phối hợp

- Maven main: `org.example.eshop.commerce.CommerceApplication`; bật profile commerce, chỉ scan commerce Entity/repository/component, tắt AI auto-configuration và validate schema mà không sửa bảng.
- Backend mới: `src/main/java/org/example/eshop/commerce/`; frontend: `frontend/`.
- Code legacy còn tại package/controller cũ và được tách khỏi commerce. Có thể gọi main class EshopApplication riêng khi làm regression với schema legacy; không dùng nó để chạy schema mới.
- Contract máy đọc: `docs/api/tv3-commerce.yaml`; thỏa thuận adapter: `docs/api/TV3_IMPLEMENTATION_CONTRACT.md`.
- TV1/TV2/TV4 chưa cung cấp module mới: các JDBC adapter chỉ đọc tài khoản/quyền/catalog/tồn từ bảng thật, không tự sửa dữ liệu của thành viên khác. Khi có service chính thức, thay implementation port và kiểm tra contract. Đây là đề xuất kỹ thuật có implementation cục bộ, chưa có xác nhận của từng thành viên nhóm.
- Session dùng userId do DB cấp, roles/status được nạp lại theo request. Không có tài khoản mặc định hoặc lối đăng nhập giả trong app. Dev seed được chạy riêng, chỉ vào DB cô lập.

## Chuẩn bị

JDK 17+ (máy hiện tại chạy JDK21), Node phù hợp `frontend/package.json`, MariaDB 12.2 đã dùng để kiểm thử. Windows CLI MariaDB mặc định ở `C:\Program Files\MariaDB 12.2\bin\mariadb.exe`; script hỗ trợ `--client` nếu đường dẫn khác.

Mỗi terminal có environment riêng. Root workspace là thư mục chứa pom.xml. Không commit mật khẩu vào source hoặc biến VITE_*.

## Tạo dữ liệu demo riêng (một lần)

```powershell
$env:DB_PASSWORD = Read-Host 'Mật khẩu MariaDB'
python scripts/tv3_database.py create --database furniture_shop_tv3_dev
$env:TV3_DEMO_PASSWORD = Read-Host 'Đặt mật khẩu demo (ít nhất 10 ký tự)'
python scripts/tv3_database.py seed --database furniture_shop_tv3_dev
Remove-Item Env:TV3_DEMO_PASSWORD
```

`create` cố ý thất bại nếu DB đã tồn tại; không xóa/ghi đè DB. Nếu đã tạo, chỉ chạy `seed` khi muốn chuẩn bị lại fixture demo. Seed tạo `tv3.admin` (ADMIN), `tv3.customer` (CUSTOMER), ba SKU và tồn MAIN; mật khẩu lấy từ environment và hash bằng BCrypt, không in ra. Không dùng chung mật khẩu tài khoản thật. Script từ chối database `furniture_shop` chính.

Khi DB demo trên máy đã được chuẩn bị, không cần tạo lại. Có thể đặt lại mật khẩu demo bằng cách chạy `seed` với environment do bạn chọn. Script chỉ chấp nhận `furniture_shop_tv3_dev` hoặc `furniture_shop_test_*`.

## Terminal backend

```powershell
$env:DB_URL = 'jdbc:mariadb://localhost:3306/furniture_shop_tv3_dev'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = Read-Host 'Mật khẩu MariaDB'
.\mvnw.cmd spring-boot:run
```

Backend chạy `http://localhost:8080`. Runtime commerce không cần JWT_SECRET hoặc GEMINI_API_KEY. `GET /api/commerce/runtime` chỉ trả tên ứng dụng và cờ DB cô lập, không trả tên DB/secret. Bằng chứng session/CSRF: `/api/session` và `/api/csrf`.

## Terminal frontend

```powershell
cd frontend
npm ci
npm run dev
```

Mở `http://127.0.0.1:5173/cart`, đăng nhập tại `/login`, quản trị tại `/admin/coupons`. Vite proxy API về backend, không cần CORS wildcard. Build deploy dùng `npm run build`; reverse proxy phục vụ SPA và chuyển `/api` về Java. Đây không phải một backend trả tên template HTML.

Nếu máy Windows có CA doanh nghiệp và gặp lỗi chứng chỉ npm, có thể đặt `NODE_OPTIONS=--use-system-ca` tạm thời. Maven có thể dùng truststore Windows `-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE` trong MAVEN_OPTS. Không tắt xác minh TLS; không cần thay truststore máy lâu dài.

## Kiểm thử

Unit/security không cần MariaDB:

```powershell
.\mvnw.cmd '-Dtest=CurrentActorTest,CommerceSecurityTest,CatalogBridgeTest,CommerceRuntimeTest,CouponCalculatorTest' test
cd frontend
npm test
npm run build
```

Integration API/MariaDB thật (database riêng, test sẽ xóa fixture bên trong nó; tuyệt đối không trỏ vào dữ liệu cần giữ):

```powershell
python scripts/tv3_database.py create --database furniture_shop_test_tv3_local
$env:TV3_TEST_DB_URL = 'jdbc:mariadb://localhost:3306/furniture_shop_test_tv3_local'
.\mvnw.cmd '-Dtest=CommerceDatabaseTest' test
Remove-Item Env:TV3_TEST_DB_URL
```

`DB_PASSWORD` và `DB_USERNAME` vẫn cần trong terminal này. Integration kiểm tra tên DB trước khi dọn fixture. Ca tranh chấp gộp giỏ đọc INNODB_LOCK_WAITS/INNODB_TRX để đồng bộ chính xác theo khóa, cần quyền đọc metadata phù hợp (đã chạy bằng root trên DB test).

Smoke HTTP thật và Chrome E2E (backend/frontend đang chạy bằng DB demo):

```powershell
$env:TV3_DEMO_PASSWORD = Read-Host 'Mật khẩu tài khoản demo đã seed'
python scripts/tv3_http_smoke.py
cd frontend
npm run test:e2e
Remove-Item Env:TV3_DEMO_PASSWORD
```

Hai công cụ kiểm thử mutation xác minh runtime nối vào DB cô lập trước khi thao tác. Playwright dùng Chrome đã cài trên máy (`channel: chrome`), không điều khiển tab Chrome của người dùng. Screenshot desktop/mobile ở `frontend/test-results/`, được Git bỏ qua. Coupon do smoke/E2E tạo được xóa trong cleanup; E2E có thể để lại giỏ của tài khoản demo sau ca merge.

Full regression suite còn chứa legacy SpringBoot smoke test, cần schema legacy riêng. Trong phiên triển khai đã dùng schema trước migration từ commit 59c435b trong một DB test riêng; TV3 tests dùng DynamicPropertySource để trỏ tới schema mới. Không chạy legacy context trên `furniture_shop` chính hoặc tự tái tạo DB eshop đã xóa. Không xem legacy smoke pass là bằng chứng checkout đã migrate.

## Kịch bản demo

1. Khách mở giỏ, tìm/thêm SKU, tăng/giảm, refresh để thấy dữ liệu giữ lại, xóa dòng/xóa hết.
2. Thêm hàng rồi đăng nhập `tv3.customer`/`tv3.admin`: giỏ khách gộp vào tài khoản, refresh vẫn còn. Đăng xuất tạo lại ngữ cảnh giỏ khách riêng.
3. Với admin, tạo coupon PERCENT/FIXED, cap, minimum, limits và thời hạn; sửa trường nullable về trống, xem thử số tiền, tắt/bật mã.
4. Xóa coupon chưa dùng; với dữ liệu đã có tham chiếu, backend trả COUPON_IN_USE và giữ nguyên lịch sử. Tình huống này đã được test tự động bằng order fixture trong DB test, vì API tạo đơn nằm ngoài phạm vi hiện tại.
5. Giới thiệu OpenAPI, test quyền/CSRF, test thêm/gộp đồng thời và ảnh desktop/mobile. Nêu rõ bước tiếp theo là TV3-04 xem trước checkout.
