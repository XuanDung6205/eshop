# Nếp Nhà — React commerce

Phạm vi TV3-01/02/03: giỏ hàng, đăng nhập/gộp giỏ khách và quản lý mã ưu đãi. Dùng dữ liệu thật từ API; không có dữ liệu mẫu trong giao diện. Đặt hàng/thanh toán chưa triển khai.

## Chạy local

Yêu cầu Node 22.12+ thuộc nhánh 22, hoặc Node 24+ (đã kiểm tra với Node 24.13.0), npm và backend commerce ở `http://localhost:8080`.

```powershell
cd frontend
npm ci
npm run dev
```

Mở `http://127.0.0.1:5173/cart`, `/login`, hoặc `/admin/coupons`. Vite chuyển tiếp `/api` đến backend; luôn gọi API với cookie phiên và CSRF. Không cấu hình token/password trong frontend. Dùng tài khoản thật hoặc dữ liệu test cô lập do backend cung cấp; frontend không tạo tài khoản, sản phẩm hay kho.

Nếu npm báo lỗi chứng chỉ trên Windows có chứng chỉ doanh nghiệp, có thể dùng kho chứng chỉ hệ điều hành của Node:

```powershell
$env:NODE_OPTIONS='--use-system-ca'
npm ci
```

Không tắt kiểm tra TLS. Tránh mở dev server ra mạng công cộng.

```powershell
npm test
npm run build
npm run preview
```

Build nằm trong `dist/`. Khi triển khai cần cùng origin, reverse proxy `/api` đến Java, và SPA fallback về `index.html` cho các route React. Không đưa biến bí mật vào các biến `VITE_*` vì chúng nằm trong bundle công khai.

## Hành vi tích hợp

- Tiền do server trả ở dạng chuỗi thập phân; frontend chỉ định dạng, không tính giá/tổng/giảm giá.
- Đăng nhập gửi form URL-encoded, lấy CSRF mới, sau đó gọi `POST /api/cart/merge`. Lỗi gộp không hủy phiên đăng nhập; có nút thử lại và đường dẫn điều chỉnh giỏ tài khoản. Sau đăng xuất cũng lấy CSRF mới.
- Coupon yêu cầu STAFF/ADMIN. Form giữ tất cả trường chỉnh sửa; ô trống ở giới hạn/cap gửi `null`. Thời gian được hiển thị theo múi giờ trình duyệt và chuyển sang UTC, giữ cả giây/mili giây khi sửa. Trạng thái “Đang bật” chỉ phản ánh `isActive`; thời hạn được hiển thị riêng.
- `COUPON_IN_USE` khi xóa hiển thị thông báo server và giữ thao tác tắt mã. Nút xem thử dùng API tính giảm; không phải giữ lượt hay xác nhận checkout.

## Bằng chứng kiểm thử frontend

- RED trước implementation: 13 test fail (thiếu màn hình, session/CSRF client, định dạng tiền và UTC).
- GREEN: `npm test` chạy 19 test/3 file, tất cả pass. Bao gồm giỏ/số lượng/xóa/xóa hết, catalog tìm/thêm, đăng nhập/gộp và khôi phục lỗi gộp, phân quyền, tạo/sửa đầy đủ coupon, null, UTC chính xác, lỗi theo trường, PATCH tắt mã, tìm/phân trang, FK conflict và preview server. Test hồi quy thời gian cũng đã chạy RED trước khi giữ nguyên UTC khi người dùng không đổi giờ địa phương (tránh thay đổi giờ lặp lại khi chuyển DST).
- `npm run build`: pass. `npm audit`: 0 lỗ hổng sau khi ghim phiên bản đã vá.
- Component test chỉ thay thế biên HTTP; không chứng minh transaction, quyền sở hữu hoặc MariaDB. Trên môi trường Windows Codex này Vitest cần chạy ngoài sandbox do sandbox chặn rename tệp tạm (`EPERM`).
- Smoke qua proxy Vite thật tới Java/MariaDB cô lập `furniture_shop_tv3_dev`: `/api/session` trả guest, `/api/cart` trả giỏ rỗng/VND và catalog trả 3 SKU đã seed, giá có kiểu chuỗi. Chỉ kiểm tra đọc qua HTTP, không thay thế E2E browser.
- E2E thực tế: `npm run test:e2e` qua Playwright dùng Chrome đã cài (`channel: chrome`), backend Java và MariaDB demo thật. Hai test pass: giỏ khách/thêm/sửa/refresh/xóa; đăng nhập/gộp giỏ và quản trị coupon tạo/preview/sửa/tắt/xóa/đăng xuất. Kiểm tra desktop 1440px và mobile 390px không tràn ngang, không có pageerror. Ảnh nằm trong `test-results/` (Git bỏ qua), đã được xem trực tiếp để kiểm tra bố cục.
- E2E chỉ chạy mutation sau khi `/api/commerce/runtime` xác nhận DB cô lập. Cần đặt `TV3_DEMO_PASSWORD` qua environment và khởi chạy backend/Vite trước; test không tự sinh người dùng, không dùng DB production. Trường hợp callback/payment/checkout ngoài phạm vi TV3-01/02/03.

Tài liệu thư viện đã đối chiếu: [Vite](https://vite.dev/guide/), [React Router](https://reactrouter.com/start/declarative/installation), [Vitest](https://vitest.dev/guide/) và metadata/audit từ npm registry chính thức. Các phiên bản đã khóa trong `package-lock.json`.
