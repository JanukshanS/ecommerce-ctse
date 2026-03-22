# Cart Service — Functionality Overview

The **cart service** manages per-user shopping carts in the ecommerce microservices stack.

## REST API (`/api/cart`)

All endpoints require an **`X-User-Id`** header to identify the user’s cart.

| Method | Path | Description |
|--------|------|-------------|
| **GET** | `/api/cart` | Returns the user’s cart. Creates an empty cart in the database if none exists. |
| **POST** | `/api/cart/items` | Adds a line item, or increases quantity if the product is already in the cart. |
| **PUT** | `/api/cart/items/{productId}` | Updates quantity for that product. Quantity **0** removes the line. |
| **DELETE** | `/api/cart/items/{productId}` | Removes that product from the cart. |
| **DELETE** | `/api/cart` | Clears the entire cart (empty items, total = 0). |

## Business behavior

- **Catalog integration** — Before adding an item, the service loads the product from the **catalog service** and uses catalog **name** and **price** when available (falls back to request body values if needed).
- **Stock validation** — Uses the catalog client’s stock check so the total quantity in the cart cannot exceed available stock.
- **Totals** — Recomputes **`totalAmount`** from `price × quantity` for all line items (rounded to two decimal places).
- **Persistence** — Carts are stored in **MongoDB** per `userId`, with `createdAt` and `updatedAt` timestamps.

## Response shape (`CartResponse`)

| Field | Description |
|-------|-------------|
| `cartId` | Cart document identifier |
| `userId` | Owner of the cart |
| `items` | List of cart line items |
| `totalAmount` | Sum of line totals |
| `itemCount` | Number of distinct line items |
| `createdAt` | Cart creation time |
| `updatedAt` | Last update time |

## OpenAPI

The HTTP contract is also described in **`openapi.yaml`** in this folder (e.g. direct service URL `http://localhost:8083` when running the service standalone).

## Summary

Per-user **shopping cart** with **add / update / remove / clear** operations, **catalog-backed** product data, **stock validation**, and **persisted totals**.
