# API Gateway — End-to-end testing guide

Use **one base URL** for all requests. Default: **`http://localhost:8080`**.  
If you mapped the gateway to another host port (e.g. `8090:8080` in Docker), use that port instead.

---

## 1. How authentication works

| Rule | Detail |
|------|--------|
| **Header for protected routes** | `Authorization: Bearer <JWT>` |
| **No manual `X-User-Id`** | The gateway validates the JWT and adds **`X-User-Id`** and **`X-Username`** for downstream services. |
| **Public (no JWT)** | `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/validate` |
| **Public catalog reads** | **GET** requests under `/api/catalog/products/**` (browse/search/stock-check) — no JWT |
| **Everything else** | Valid **Bearer** token required |

After **register** or **login**, save:

- **`token`** → use in `Authorization: Bearer {{token}}`
- **`userId`** → useful for your notes (gateway still derives it from the JWT)

---

## 2. Recommended test flow

```
1. Register (or Login)     → get JWT + userId
2. Create a product        → POST catalog (JWT) — save productId from response
3. (Optional) Browse       → GET catalog — no JWT
4. Add to cart             → POST cart (JWT)
5. (Optional) Cart CRUD    → GET / PUT / DELETE cart (JWT)
6. Place order             → POST orders (JWT) — triggers internal payment (~90% success); may clear cart on success
7. Inspect order / payment → GET orders, GET payments (JWT where required)
8. (Optional) Refund       → POST payments refund (JWT) — only if payment status is SUCCESS
```

**Note:** Creating an order **automatically** calls the payment service (credit card, internal). You do **not** need to call `POST /api/payments/process` for that path. Use `POST /api/payments/process` only if you want to test the payment API **directly** (e.g. custom amount/method).

Use the **same user** for seller flows (create product) and buyer flows (cart, order) when testing alone. For seller-only rules (update/delete product), the **`X-User-Id`** from your JWT must match the product’s seller.

---

## 3. Variables (Postman / Insomnia)

| Variable | Set after… |
|----------|------------|
| `baseUrl` | `http://localhost:8080` |
| `token` | Register or Login |
| `userId` | Register or Login (optional; for documentation) |
| `productId` | Create product |
| `orderId` | Create order |
| `paymentId` | Payment response or GET payment by order |

---

## 4. Auth

### 4.1 Register

**POST** `{{baseUrl}}/api/auth/register`  
**Headers:** `Content-Type: application/json`  
**Body:**

```json
{
  "username": "testuser",
  "email": "testuser@example.com",
  "password": "secret12"
}
```

**Response (201):** includes `token`, `tokenType` (e.g. `Bearer`), `userId`, `username`, `email`, `roles`.

---

### 4.2 Login

**POST** `{{baseUrl}}/api/auth/login`

```json
{
  "username": "testuser",
  "password": "secret12"
}
```

**Response (200):** same shape as register.

---

### 4.3 Validate token

**GET** `{{baseUrl}}/api/auth/validate`  
**Headers:** `Authorization: Bearer {{token}}`

**Response (200):** e.g. `{ "valid": true, "userId": "...", "username": "...", "roles": ["ROLE_USER"] }`

---

## 5. Catalog (`/api/catalog/products`)

### 5.1 Create product (JWT required)

**POST** `{{baseUrl}}/api/catalog/products`  
**Headers:**

- `Content-Type: application/json`
- `Authorization: Bearer {{token}}`

**Body:**

```json
{
  "name": "Wireless Mouse",
  "description": "Ergonomic wireless mouse with USB receiver.",
  "price": 29.99,
  "category": "Electronics",
  "stock": 100
}
```

**Response (201):** product object — **save `id` as `productId`**.

---

### 5.2 List products (no JWT)

**GET** `{{baseUrl}}/api/catalog/products?page=0&size=20&sort=createdAt,desc`

---

### 5.3 Get product by id (no JWT)

**GET** `{{baseUrl}}/api/catalog/products/{{productId}}`

---

### 5.4 Products by category (no JWT)

**GET** `{{baseUrl}}/api/catalog/products/category/Electronics?page=0&size=20`

---

### 5.5 Search (no JWT)

**GET** `{{baseUrl}}/api/catalog/products/search?q=mouse&page=0&size=20`

---

### 5.6 Stock check (no JWT)

**GET** `{{baseUrl}}/api/catalog/products/{{productId}}/stock-check?quantity=2`

**Response (200):** e.g. `{ "productId": "...", "requestedQuantity": 2, "available": true }`

---

### 5.7 Update product (JWT — must be owner)

**PUT** `{{baseUrl}}/api/catalog/products/{{productId}}`  
**Headers:** `Authorization: Bearer {{token}}`, `Content-Type: application/json`

```json
{
  "name": "Wireless Mouse Pro",
  "description": "Updated description.",
  "price": 34.99,
  "category": "Electronics",
  "stock": 80
}
```

---

### 5.8 Delete product (soft, JWT — must be owner)

**DELETE** `{{baseUrl}}/api/catalog/products/{{productId}}`  
**Headers:** `Authorization: Bearer {{token}}`

**Response:** `204 No Content`

---

## 6. Cart (`/api/cart`)

All cart routes require **JWT** (gateway supplies `X-User-Id`).

### 6.1 Get cart

**GET** `{{baseUrl}}/api/cart`  
**Headers:** `Authorization: Bearer {{token}}` (gateway adds `X-User-Id` from the JWT)

**Optional query:** `{{baseUrl}}/api/cart?userId={{userId}}` — same user as the token; if you send both `X-User-Id` and `userId`, they must match.

---

### 6.2 Add item

**POST** `{{baseUrl}}/api/cart/items`  
**Headers:** `Authorization: Bearer {{token}}`, `Content-Type: application/json`

```json
{
  "productId": "<paste-product-id-from-catalog>",
  "productName": "Wireless Mouse",
  "price": 29.99,
  "quantity": 2
}
```

Catalog is validated and stock is checked; name/price may be overridden from catalog.

---

### 6.3 Update item quantity

**PUT** `{{baseUrl}}/api/cart/items/{{productId}}`  
**Headers:** `Authorization: Bearer {{token}}`, `Content-Type: application/json`

```json
{
  "quantity": 3
}
```

Use `quantity: 0` to remove the line.

---

### 6.4 Remove item

**DELETE** `{{baseUrl}}/api/cart/items/{{productId}}`  
**Headers:** `Authorization: Bearer {{token}}`

---

### 6.5 Clear cart

**DELETE** `{{baseUrl}}/api/cart`  
**Headers:** `Authorization: Bearer {{token}}`

**Response:** `204 No Content`

---

## 7. Orders (`/api/orders`)

All routes below use **JWT** except where noted.

### 7.1 Create order (triggers payment + may clear cart)

**POST** `{{baseUrl}}/api/orders`  
**Headers:** `Authorization: Bearer {{token}}`, `Content-Type: application/json`

```json
{
  "items": [
    {
      "productId": "<product-id>",
      "productName": "Wireless Mouse",
      "price": 29.99,
      "quantity": 2
    }
  ],
  "shippingAddress": "123 Main St, City, Country",
  "notes": "Leave at door"
}
```

- `notes` is optional (can be omitted or `null`).
- Payment is invoked internally (simulated ~90% success). On success, order becomes **CONFIRMED** / **PAID** and the **cart is cleared**.

**Response (201):** order — **save `id` as `orderId`**, and `paymentId` if present.

---

### 7.2 List my orders

**GET** `{{baseUrl}}/api/orders?page=0&size=10`  
**Headers:** `Authorization: Bearer {{token}}`

---

### 7.3 Get order by id

**GET** `{{baseUrl}}/api/orders/{{orderId}}`  
**Headers:** `Authorization: Bearer {{token}}`

---

### 7.4 Update order status

**PUT** `{{baseUrl}}/api/orders/{{orderId}}/status`  
**Headers:** `Content-Type: application/json`  
**Note:** Controller does not require `X-User-Id`; still send **JWT** if your gateway applies the filter to this path (it does).

**Body example:**

```json
{
  "status": "PROCESSING",
  "paymentStatus": "PAID",
  "paymentId": "<optional-payment-id>"
}
```

**Enums:**

- `status`: `PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`
- `paymentStatus` (order service): `PENDING`, `PAID`, `FAILED`, `REFUNDED`

All fields in the body are optional; send only what you want to update.

---

### 7.5 Cancel order

**DELETE** `{{baseUrl}}/api/orders/{{orderId}}`  
**Headers:** `Authorization: Bearer {{token}}`

---

## 8. Payments (`/api/payments`)

### 8.1 Process payment (manual test)

**POST** `{{baseUrl}}/api/payments/process`  
**Headers:** `Authorization: Bearer {{token}}`, `Content-Type: application/json`

```json
{
  "orderId": "<order-id>",
  "amount": 59.98,
  "method": "CREDIT_CARD",
  "description": "Manual test payment"
}
```

**`method` values:** `CREDIT_CARD`, `DEBIT_CARD`, `PAYPAL`, `BANK_TRANSFER`

**Response (201):** payment — **save `id` as `paymentId`**.  
`status` is payment-service enum: `PENDING`, `SUCCESS`, `FAILED`, `REFUNDED`.

---

### 8.2 Get payment by order id

**GET** `{{baseUrl}}/api/payments/order/{{orderId}}`  
**Headers:** `Authorization: Bearer {{token}}`

---

### 8.3 Payment history

**GET** `{{baseUrl}}/api/payments/history?page=0&size=10`  
**Headers:** `Authorization: Bearer {{token}}`

---

### 8.4 Refund

**POST** `{{baseUrl}}/api/payments/{{paymentId}}/refund`  
**Headers:** `Authorization: Bearer {{token}}`, `Content-Type: application/json`

```json
{
  "reason": "Customer requested refund"
}
```

Only allowed if the payment belongs to the user and status is **SUCCESS**.

---

## 9. Quick `curl` sequence (bash)

Replace `BASE` and paste token after login.

```bash
BASE=http://localhost:8080

# Register
curl -s -X POST "$BASE/api/auth/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","email":"demo@example.com","password":"secret12"}'

# Login → copy "token" from JSON
TOKEN="<paste-jwt-here>"

# Create product
curl -s -X POST "$BASE/api/catalog/products" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Item","description":"Desc","price":10.0,"category":"Misc","stock":50}'

# Set PRODUCT_ID from response "id"
PRODUCT_ID="<paste-id>"

# Add to cart
curl -s -X POST "$BASE/api/cart/items" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"productId\":\"$PRODUCT_ID\",\"productName\":\"Test Item\",\"price\":10.0,\"quantity\":1}"

# Create order (auto payment)
curl -s -X POST "$BASE/api/orders" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"items\":[{\"productId\":\"$PRODUCT_ID\",\"productName\":\"Test Item\",\"price\":10.0,\"quantity\":1}],\"shippingAddress\":\"1 Test St\"}"
```

---

## 10. JWT secret (local)

The gateway decodes the JWT with **`jwt.secret`**. It must be a **Base64-encoded** key (see `api-gateway` `application.yml` and your `.env` / Docker env). Auth and gateway must use the **same** secret, or tokens will be rejected.

---

## 11. Health checks (optional)

- Gateway: `GET {{baseUrl}}/actuator/health`  
- Per-service Swagger (direct ports in Docker): e.g. `http://localhost:8081/swagger-ui.html` (not routed through gateway unless configured).

---

*Generated from the controllers and DTOs in this repository (`api-gateway`, `auth-service`, `catalog-service`, `cart-service`, `order-service`, `payment-service`).*
