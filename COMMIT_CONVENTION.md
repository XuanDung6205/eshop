# Quy ước commit

Dự án sử dụng **Conventional Commits** để lịch sử thay đổi dễ đọc và tra cứu. Mỗi commit nên tập trung vào một thay đổi có liên quan.

## Cấu trúc

```text
<type>(<scope>): <summary>
```

- `type` là loại thay đổi, bắt buộc.
- `scope` là phần bị ảnh hưởng, không bắt buộc.
- `summary` mô tả ngắn gọn thay đổi; dùng tiếng Anh, bắt đầu bằng động từ, viết thường sau dấu `:` và không kết thúc bằng dấu chấm.
- Dùng `!` sau `type` hoặc `scope` để đánh dấu thay đổi không tương thích ngược.

Ví dụ:

```text
feat(cart): add item quantity update
fix(auth): reject expired access tokens
```

## Các loại commit

| Type | Khi sử dụng |
| --- | --- |
| `feat` | Thêm chức năng mới |
| `fix` | Sửa lỗi |
| `docs` | Thay đổi tài liệu |
| `style` | Định dạng hoặc chỉnh sửa trình bày, không đổi logic |
| `refactor` | Tái cấu trúc mà không thêm tính năng hoặc sửa lỗi |
| `test` | Thêm hoặc cập nhật kiểm thử |
| `perf` | Cải thiện hiệu năng |
| `build` | Thay đổi Maven, dependency hoặc cấu hình build |
| `ci` | Thay đổi cấu hình tích hợp/triển khai liên tục |
| `chore` | Công việc bảo trì khác không thuộc các loại trên |
| `revert` | Hoàn tác một commit trước đó |

## Scope gợi ý

Chọn scope ngắn, phản ánh khu vực chính bị thay đổi. Có thể bỏ scope nếu thay đổi ảnh hưởng nhiều khu vực.

- `auth`: đăng nhập, đăng ký và xác thực
- `security`: phân quyền và cấu hình bảo mật
- `product`: sản phẩm
- `cart`: giỏ hàng
- `order`: đơn hàng
- `ai`: trợ lý AI
- `api`: API và DTO
- `config`: cấu hình ứng dụng
- `test`: kiểm thử

## Nội dung bổ sung

Khi cần giải thích lý do hoặc tác động, thêm phần body sau một dòng trống. Dùng footer để tham chiếu issue hoặc mô tả breaking change.

```text
feat(order): add order status tracking

Expose the current status in the order details response.

Refs: #123
```

Với thay đổi không tương thích ngược, dùng `!` và nêu rõ tác động trong footer:

```text
feat(api)!: rename product response fields

BREAKING CHANGE: clients must use `productName` instead of `name`.
```

## Ví dụ theo dự án

```text
feat(product): add product search
fix(cart): prevent adding unavailable products
test(auth): cover invalid login credentials
docs: add commit conventions
build: update Spring AI dependency
```
