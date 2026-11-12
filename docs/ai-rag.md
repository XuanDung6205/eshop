# Product RAG assistant

`POST /api/chat` now runs the complete product RAG flow and can personalize product retrieval from recent tracking:

1. Combine the customer's question with recent searches and interacted products (when available), then search the MariaDB Vector Store.
2. Read the matching product IDs from vector metadata.
3. Load the products again from the `product` table. This keeps price and stock current even when a vector was created earlier.
4. Send only that catalogue context to Gemini through `AiChatService`.

When the current question explicitly names a catalogue category, retrieved products are restricted to that category and completed with in-stock products from the same category when needed. That current category takes precedence over older conversation topics. If the current question omits a category, the recent conversation can supply one. For questions without an explicit category, weak vector matches below the configured similarity threshold are discarded.

The response contains the generated `reply`, a `conversationId`, and the `sources` used by the answer. Every source includes the current `price`, `quantity`, product `name`, and the storefront URL (`/products/{id}`). When no current product matches, the service asks the customer for a more specific requirement and does not call the model.

## Local configuration

The cloud embedding and chat models need a Google Gemini API key. Keep it outside Git:

```bash
export GEMINI_API_KEY='your-key'
```

The configured database is now `furniture_shop`; see `docs/database/README.md` for its versioned bootstrap. Legacy database initialization scripts have been removed, and automatic SQL/vector schema initialization is disabled. This document describes the existing Java RAG flow, which still needs to be migrated to the furniture entities before running against the new database. Create the Spring AI vector-store schema through a dedicated migration at that stage. The first indexing run calls the embedding model and therefore also needs `GEMINI_API_KEY`.

Without a key, unit tests that mock the AI and vector services still run, but a live RAG request cannot create embeddings or call Gemini. The key must never be committed to `application.properties`.

Example request:

```http
POST /api/chat
Content-Type: application/json

{"message":"Tư vấn điện thoại chụp ảnh tốt dưới 20 triệu","sessionId":"123e4567-e89b-12d3-a456-426614174000"}
```

For guest personalization, send the UUID stored as `eshop.trackingSessionId` in browser `sessionStorage`. Signed-in requests also use the user's recent tracking history. The original message remains the question sent to Gemini; history only helps retrieve more relevant catalogue products.

The optional `context` request property is retained only for compatibility with older clients. The server ignores it and always builds trusted context from the vector search and current product rows.
