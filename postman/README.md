# Postman — import instructions

## Files

| File | Purpose |
|------|---------|
| **`ecommerce-ms.postman_collection.json`** | Collection v2.1 — import this into Postman (**File → Import**). |
| **`ecommerce-ms-local.postman_environment.json`** | Optional environment with `baseUrl` and variable placeholders. |

## Quick start

1. Open Postman → **Import** → select **`ecommerce-ms.postman_collection.json`**.
2. (Recommended) Import **`ecommerce-ms-local.postman_environment.json`** and select **ECommerce Local** in the environment dropdown.
3. Start Docker / API Gateway on port **8080** (or change `baseUrl` in the environment / collection variables).
4. Run **01 - Auth → Register User** (or **Login** if you already registered). The **Tests** tab saves `token` and `userId`.
5. Run folders **02 → 06** in order for a full happy path.
6. Run **07 - Cleanup** only when finished — it soft-deletes the main product used in earlier steps.

## Variables

- **`baseUrl`**: default `http://localhost:8080` (API Gateway).
- **`token`**, **`userId`**, **`productId`**, **`productId2`**, **`orderId`**, **`paymentId`**: filled by request **Tests** scripts (saved to both the active **environment** and **collection** variables when possible).

If an environment variable is **empty**, it can override a collection value in Postman. Prefer selecting **ECommerce Local** after import, or clear conflicting keys.

## More detail

See **`API_GATEWAY_TESTING.md`** in the repository root for JSON bodies, headers, and endpoint notes.
