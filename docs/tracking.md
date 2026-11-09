# TV4 User Tracking contract

The browser sends `POST /tracking/events` with JSON. This endpoint accepts guests and signed-in customers through the web security chain; it requires the normal CSRF header. The browser supplies an opaque UUID `sessionId`, which is only for analytics. The server derives `userId` from the authenticated principal and never accepts one from JSON.

Example:

```json
{
  "eventType": "SEARCH",
  "sessionId": "dfaa68f0-ec69-41bf-a28d-b6532e498a35",
  "occurredAt": "2026-11-09T09:00:00Z",
  "keyword": "áo khoác",
  "categoryId": 2,
  "resultCount": 0,
  "pagePath": "/products"
}
```

The endpoint returns `202 Accepted` for a valid event, `400 Bad Request` for invalid data or server-owned event types, and `403 Forbidden` without a valid CSRF token. It does not return stored event data.

On startup, `db/tracking-schema.sql` creates the `tracking_event` table if it is absent. The existing `spring.jpa.hibernate.ddl-auto=none` setting remains in force; Hibernate does not alter the rest of the database schema.

| Event type | Recorded by | Meaning |
| --- | --- | --- |
| `PAGE_VIEW` | Browser | Storefront page loaded |
| `PRODUCT_VIEW` | Browser | Product detail loaded |
| `SEARCH` | Browser | Search results loaded for a nonempty keyword; includes `resultCount` |
| `FILTER` | Browser | Category-filtered results loaded; includes `resultCount` |
| `ADD_TO_CART_CLICK` | Browser | Add form submitted; may fail |
| `CART_UPDATE_CLICK` | Browser | Quantity update form submitted; may fail |
| `CART_REMOVE_CLICK` | Browser | Remove form submitted |
| `CART_CLEAR_CLICK` | Browser | Clear cart form submitted |
| `CHECKOUT_START` | Browser | Checkout page loaded |
| `ADD_TO_CART` | Server | Product added to cart successfully |
| `CART_UPDATE` | Server | Cart quantity changed successfully |
| `CART_REMOVE` | Server | Product removed from cart successfully |
| `CART_CLEAR` | Server | Nonempty cart cleared successfully |
| `ORDER_PLACED` | Server | Order committed successfully; includes `orderId` |

All events include `occurredAt` and `receivedAt` in UTC, plus `sessionId`. Optional dimensions are `productId`, `categoryId`, `keyword`, `resultCount`, `quantity`, and `pagePath`. `ORDER_PLACED` can be joined with `orders` and `order_detail` by `orderId` for product-level funnel metrics. The tracking table intentionally keeps product and order IDs as values so deleting a product or order does not erase event history.

Browser events are analytics signals, not a source of truth for completed actions. The UI never sends passwords, payment details, customer contact fields, JWTs, or `userId`. Search terms are limited to 200 characters, but they are user-entered text; analytics consumers should not treat them as safe display HTML.
