# TV3-01/02/03 implementation contract

This is the concrete local integration contract for the approved first three tasks. Adjacent team modules have not been delivered yet; JDBC adapters read their existing MariaDB tables without rewriting their entities or writing their data. These adapters are replaceable implementations of the ports below, not mock data. No claim of teammate sign-off is made.

## Runtime boundary

- New main class: `org.example.eshop.commerce.CommerceApplication`, scanning only `org.example.eshop.commerce`. Default Maven main points here. Legacy MVC/AI code remains available separately and is excluded from commerce startup.
- Frontend: `frontend/`, React/Vite, browser routes `/cart`, `/admin/coupons`, `/login`; Vite proxies `/api` to localhost:8080. No checkout/payment implementation in this scope.
- Session authentication over HttpOnly JSESSIONID cookie. JSON login uses URL-encoded form fields username/password. Login/logout require CSRF; unsafe guest-cart requests also require CSRF.
- `GET /api/csrf` -> `{token, headerName}`; client gets a fresh token after login/logout. No globally disabled CSRF or CORS wildcard with credentials.
- `GET /api/session` -> `{authenticated:boolean,user:null|{id,username,fullName,roles:["ADMIN"|"STAFF"|"CUSTOMER"]}}`.
- `POST /api/session/login` and `POST /api/session/logout` -> same session shape; login failure is JSON 401. Existing users/roles are read, not managed here. Disabled accounts and changed roles are revalidated on subsequent requests.
- `/api/admin/coupons/**` requires STAFF or ADMIN. Cart API is guest/customer/admin accessible, with ownership derived server-side, never selected by body IDs.
- Error: `{code:string,message:string,fieldErrors:object}`; 400 invalid request, 401 authentication required, 403 insufficient role/CSRF, 404 unavailable resource/foreign cart line, 409 business conflict.
- Money JSON fields are decimal strings, e.g. `"1250000.00"`; no client-side monetary calculations. UTC times use ISO-8601 with Z. page >= 0, size 1..100; clamp/validate before querying.

## Java integration ports (under commerce.port)

- `CommerceActor(Long userId, byte[] guestHash, Set<String> roles)`; userId may be null. Authenticated actor can retain guestHash for explicit merge. Guest raw token is server-generated and HttpOnly cookie; DB stores only SHA-256 hash.
- `CurrentActorPort.currentActor(): CommerceActor`; `clearGuestIdentity(): void` invalidates guest cookie after successful merge.
- `VariantSnapshot(Long id, Long productId, String sku, String productName, String variantName, BigDecimal price, String imageUrl, boolean active)`.
- `CatalogPort.findVariants(Set<Long> ids): Map<Long,VariantSnapshot>`; includes inactive variants to display old cart rows. Batch JDBC adapter joins products/product_variants and selects image, never overwrites catalog.
- `InventoryPort.availableQuantities(Set<Long> ids): Map<Long,Integer>`; sum on_hand-reserved across active warehouses, default missing SKU stock to 0. No reservation in carts.
- Common `CommerceException.badRequest(code,message)`, `.notFound(message)`, `.conflict(code,message)`, `.forbidden(message)` and an exception handler returning the error shape. Clock bean is UTC.

## Cart API

- `GET /api/cart` -> Cart; empty cart can have id=null and does not require a new DB row.
- `POST /api/cart/items` body `{variantId:Long,quantity:int}` -> Cart (200).
- `PATCH /api/cart/items/{itemId}` body `{quantity:int}` -> Cart (200).
- `DELETE /api/cart/items/{itemId}` and `DELETE /api/cart/items` -> Cart (200).
- `POST /api/cart/merge` requires authentication; merge guest into current user, no target user ID accepted; returns Cart and clears guest token after successful transaction.
- Cart: `{id,itemCount,subtotal,currency:"VND",items,warnings}`; itemCount is sum of quantities.
- Line: `{id,variantId,productId,sku,productName,variantName,imageUrl,quantity,unitPrice,lineTotal,availableQuantity,active,issues:[string]}`.
- Warning: `{code,message,itemId}`. Codes include OUT_OF_STOCK, INSUFFICIENT_STOCK, UNAVAILABLE_VARIANT.
- Quantities 1..1000000, check overflow. Add/new increases require an active SKU and enough current availability; reducing existing quantity remains possible. Merge preserves excess-stock/inactive lines and reports issues, does not silently discard them. Hard quantity overflow rolls back the merge.
- Mutations lock cart roots, creation handles active-cart unique races. Merge locks both roots in ascending ID order and rechecks guest ACTIVE state to prevent consuming it twice.
- Lookup bridge: `GET /api/commerce/catalog/variants?query=&page=0&size=12` -> `{items,page,size,totalElements,totalPages}`; items have id/productId/sku/productName/variantName/price/imageUrl/active/availableQuantity. Read-only convenience until TV2's public catalog API is integrated.

## Coupon API

- `GET /api/admin/coupons?query=&page=0&size=20` -> Page of Coupon; `GET /api/admin/coupons/{id}` -> Coupon.
- `POST /api/admin/coupons` -> Coupon (201); `PATCH /api/admin/coupons/{id}` -> Coupon (200), supports partial updates including setting nullable fields to null; reject unknown fields.
- `DELETE /api/admin/coupons/{id}` -> 204 only when no references. FK conflicts map to JSON 409 COUPON_IN_USE with instruction to deactivate. Referenced coupon code changes are rejected (COUPON_IN_USE) to keep identifiers stable; toggle isActive instead. No cascade delete.
- Coupon fields: `{id,code,name,discountType,discountValue,maxDiscountAmount,minimumOrderAmount,totalUsageLimit,perUserLimit,startsAt,endsAt,isActive,createdAt,updatedAt}`. Money/discountValue decimal strings, dates UTC.
- Create fields: code required [A-Z0-9_-], max 50, canonical uppercase trimmed; discountType PERCENT/FIXED; discountValue >0 and PERCENT <=100; optional maxDiscountAmount >0; minimumOrderAmount >=0 default 0; optional usage limits >0; required startsAt/endsAt with end>start; isActive default true. Reject excessive scale >2 or precision >15 for monetary inputs instead of silent DB rounding. Name optional max150. PATCH never accepts id/timestamps.
- CouponCalculator uses injected Clock, startsAt inclusive / endsAt exclusive, active flag and minimum amount. Base is nonnegative merchandise remaining after line discounts, excludes shipping/assembly/tax. PERCENT/FIXED result is min(base,maxDiscount if present,computed discount), scale 2 HALF_UP. In this scope it does not reserve usage; concurrency/usage redemption is TV3-05.
- Admin page may preview a coupon against an entered merchandise base via `POST /api/admin/coupons/{id}/preview` body `{merchandiseAmount:"1000000.00"}` -> `{discountAmount,remainingAmount,currency:"VND"}`. This is authenticated read/calculation, not checkout eligibility or reservation.

## Verification and local data

- Unit and MariaDB integration tests: ownership, CSRF/roles, concurrent cart add/creation/merge, quantity bounds, expired/inactive lines, coupon validity/math/partial PATCH/referenced DELETE and stale snapshots.
- Isolated dev/test databases use the actual approved 45-table schema. A seed helper may create clearly named demo users/variants/inventory there only; passwords must be supplied through environment, never committed or printed. Do not seed or reset the user's furniture_shop database.
- Frontend component tests mock only HTTP boundaries. Browser/API end-to-end must also run against the actual local Java/MariaDB runtime.
