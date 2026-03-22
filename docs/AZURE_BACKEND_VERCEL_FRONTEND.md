# Azure backend + Vercel frontend — deployment, system & API reference

Single document for hosting the **API on Azure** and **Next.js on Vercel**, plus **system architecture**, **all gateway APIs**, **JSON payloads**, and **user flows** for frontend implementation.

**Base URL (examples):**

- Local: `http://localhost:8080`
- Azure: `https://<your-gateway>.azurecontainerapps.io` (your real URL)
- Next.js: `process.env.NEXT_PUBLIC_API_BASE_URL` — **no trailing slash**

All paths below are appended to that base (e.g. `${NEXT_PUBLIC_API_BASE_URL}/api/auth/login`).

---

## Table of contents

1. [Deployment & URLs (Vercel / Azure)](#1-deployment--urls-vercel--azure)  
2. [System overview](#2-system-overview)  
3. [End-to-end request flow](#3-end-to-end-request-flow)  
4. [Authentication & gateway rules](#4-authentication--gateway-rules)  
5. [API reference (data & endpoints)](#5-api-reference-data--endpoints)  
6. [Business & UI flows](#6-business--ui-flows)  
7. [Pagination (Spring Data)](#7-pagination-spring-data)  
8. [Next.js implementation notes](#8-nextjs-implementation-notes)  
9. [CORS](#9-cors)  
10. [Checklist & summary](#10-checklist--summary)  
11. [Related files in repo](#11-related-files-in-repo)  

---

## 1. Deployment & URLs (Vercel / Azure)

### 1.1 Architecture (hosted)

```
Browser  →  Vercel (Next.js)     →  fetch() using NEXT_PUBLIC_API_BASE_URL
                ↓
         Azure — API Gateway (:443 / :8080)  →  Auth, Catalog, Cart, Order, Payment
```

- **One public API entry:** the Azure gateway URL (or custom domain).
- The frontend **must not** call ports 8081–8085 directly from the browser.

### 1.2 Change the frontend URL easily (Vercel)

| Goal | Where |
|------|--------|
| Production shop URL | Vercel → **Settings → Domains** |
| `*.vercel.app` / previews | Automatic per deployment |

You typically **do not** embed the frontend URL in backend code. If you **restrict CORS**, add those origins on the gateway (see §9).

### 1.3 Point the app at the Azure API (easy change)

**Vercel → Settings → Environment variables:**

| Variable | Example | Notes |
|----------|---------|--------|
| `NEXT_PUBLIC_API_BASE_URL` | `https://your-gateway.azurecontainerapps.io` | No `/api` suffix |

Redeploy after changes. **Local** `.env.local`:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

### 1.4 Azure — gateway & services (typical env)

| Variable | Purpose |
|----------|---------|
| `JWT_SECRET` | Base64 secret; **same** across gateway + auth + services |
| `AUTH_SERVICE_URL` | Internal auth URL (e.g. `http://auth-service:8081`) |
| `CATALOG_SERVICE_URL` | Internal catalog |
| `CART_SERVICE_URL` | Internal cart |
| `ORDER_SERVICE_URL` | Internal order |
| `PAYMENT_SERVICE_URL` | Internal payment |
| MongoDB / `MONGODB_URI` | Per your Azure + Atlas setup |

---

## 2. System overview

### 2.1 Microservices (logical)

| Service | Direct port (local/Docker only) | Role |
|---------|----------------------------------|------|
| **API Gateway** | 8080 | JWT, routing, CORS, circuit breaker |
| **Auth** | 8081 | Register, login, validate JWT |
| **Catalog** | 8082 | Products CRUD, search, stock |
| **Cart** | 8083 | Cart per user; talks to catalog |
| **Order** | 8084 | Orders; triggers payment; clears cart on success |
| **Payment** | 8085 | Process, history, refund |

### 2.2 Tech stack (backend)

- Java 17, Spring Boot 3.2.x, Spring Cloud Gateway, Resilience4j  
- MongoDB (one DB per service)  
- JWT (jjwt), BCrypt for passwords  

### 2.3 Inter-service communication (backend only)

```
Cart     → Catalog   (product validation, stock)
Order    → Payment   (payment on order create)
Order    → Cart      (clear cart after successful payment)
Payment  → Order     (update order payment status)
```

The browser only talks to the **gateway**. The gateway adds **`X-User-Id`** and **`X-Username`** after a valid JWT (you send **`Authorization: Bearer <token>`** only).

---

## 3. End-to-end request flow

1. User opens Next.js on Vercel.  
2. **Public** actions (browse catalog): `GET` to `{BASE}/api/catalog/products/...` **without** token.  
3. **Register / login:** `POST` auth → response includes **`token`**, **`userId`**, **`username`**, **`email`**, **`roles`**. Store token (e.g. memory + `localStorage` or httpOnly cookie via Route Handler).  
4. **Protected** calls: `fetch(url, { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } })`.  
5. Gateway validates JWT → forwards to service with **`X-User-Id`** (you do not set this header from the browser).  
6. **Checkout:** `POST /api/orders` → order service creates order, calls payment internally (~simulated success/failure) → may clear cart on success.

---

## 4. Authentication & gateway rules

### 4.1 Public — no `Authorization` required

- `POST /api/auth/register`  
- `POST /api/auth/login`  
- `GET /api/auth/validate` (optional Bearer to test validity)  
- `GET` requests whose path **starts with** `/api/catalog/products` (browse, detail, search, category, stock-check)  
- `/actuator/*`, `/v3/api-docs`, `/swagger-ui` (ops/docs)

### 4.2 Protected — require `Authorization: Bearer <JWT>`

- All **non-GET** `/api/catalog/products/**` (create / update / delete product)  
- All `/api/cart/**`  
- All `/api/orders/**`  
- All `/api/payments/**`

### 4.3 Errors

- **401** from gateway: missing/invalid/expired token (JSON with `message`, etc.).

### 4.4 Roles

Gateway does **not** enforce role-based routes; new users typically get **`ROLE_USER`**. Use the same JWT for seller flows; catalog enforces **seller owns product** on update/delete via `X-User-Id`.

---

## 5. API reference (data & endpoints)

### 5.1 Auth — `/api/auth`

| Method | Path | Auth | Body / query |
|--------|------|------|----------------|
| POST | `/api/auth/register` | No | See JSON below |
| POST | `/api/auth/login` | No | See JSON below |
| GET | `/api/auth/validate` | Optional Bearer | — |

**Register body:**

```json
{
  "username": "string, 3-50 chars",
  "email": "valid email",
  "password": "string, min 6 chars"
}
```

**Login body:**

```json
{
  "username": "string",
  "password": "string"
}
```

**AuthResponse (201 register / 200 login):**

```json
{
  "token": "<jwt>",
  "tokenType": "Bearer",
  "userId": "<id>",
  "username": "string",
  "email": "string",
  "roles": ["ROLE_USER"]
}
```

**TokenValidationResponse (`GET /validate` with Bearer):**

```json
{
  "valid": true,
  "userId": "...",
  "username": "...",
  "roles": ["ROLE_USER"]
}
```

---

### 5.2 Catalog — `/api/catalog/products`

List/search/category responses use **Spring pagination** (see §7).

| Method | Path | Auth | Notes |
|--------|------|------|--------|
| GET | `/api/catalog/products` | No | Query: `page`, `size`, `sort` |
| GET | `/api/catalog/products/{id}` | No | |
| GET | `/api/catalog/products/category/{category}` | No | Query: `page`, `size`, `sort` |
| GET | `/api/catalog/products/search` | No | Query: **`q`** (required), `page`, `size` |
| GET | `/api/catalog/products/{id}/stock-check` | No | Query: **`quantity`** (int ≥ 1) |
| POST | `/api/catalog/products` | **Bearer** | Create |
| PUT | `/api/catalog/products/{id}` | **Bearer** | Owner seller only |
| DELETE | `/api/catalog/products/{id}` | **Bearer** | Soft delete; owner only |

**ProductRequest (POST/PUT body):**

```json
{
  "name": "string, required, max 200",
  "description": "string, required, max 2000",
  "price": 29.99,
  "category": "string, required, max 100",
  "stock": 100
}
```

**ProductResponse (typical fields):**  
`id`, `name`, `description`, `price`, `category`, `stock`, `sellerId`, `sellerName`, `active`, `createdAt`, `updatedAt` (ISO-8601 strings).

**Stock-check response (200):**

```json
{
  "productId": "<id>",
  "requestedQuantity": 2,
  "available": true
}
```

---

### 5.3 Cart — `/api/cart`

All require **Bearer**. Gateway sets **`X-User-Id`**.

| Method | Path | Body / params |
|--------|------|----------------|
| GET | `/api/cart` | Optional **`?userId=<same-as-jwt>`**. If both header (from gateway) and query are sent, values **must match**. At least one of header/query required. |
| POST | `/api/cart/items` | `AddToCartRequest` |
| PUT | `/api/cart/items/{productId}` | `{ "quantity": 3 }` — use `0` to remove line |
| DELETE | `/api/cart/items/{productId}` | — |
| DELETE | `/api/cart` | — **204** empty body |

**AddToCartRequest:**

```json
{
  "productId": "<catalog product id>",
  "productName": "string",
  "price": 29.99,
  "quantity": 2
}
```

**CartResponse:**  
`cartId`, `userId`, `items[]` (`productId`, `productName`, `price`, `quantity`), `totalAmount`, `itemCount`, `createdAt`, `updatedAt`.

> **Note:** Internal routes may exist for other microservices (e.g. bulk remove product from carts). The **storefront** should use only the table above via the gateway.

---

### 5.4 Orders — `/api/orders`

All require **Bearer**.

| Method | Path | Body / query |
|--------|------|--------------|
| POST | `/api/orders` | `CreateOrderRequest` |
| GET | `/api/orders` | `page`, `size` — current user’s orders |
| GET | `/api/orders/{id}` | — must belong to user |
| PUT | `/api/orders/{id}/status` | `UpdateOrderStatusRequest` (partial) |
| DELETE | `/api/orders/{id}` | Cancel |

**CreateOrderRequest:**

```json
{
  "items": [
    {
      "productId": "string",
      "productName": "string",
      "price": 10.0,
      "quantity": 1
    }
  ],
  "shippingAddress": "string, required",
  "notes": "optional string"
}
```

**Important:** **Creating an order automatically triggers payment** (order → payment service). Success is **simulated** (high probability). On **success**, order becomes paid/confirmed and **cart is cleared**. On failure, show failed payment state in UI.

**OrderResponse (key fields):**  
`id`, `userId`, `items`, `totalAmount`, `status`, `paymentStatus`, `paymentId`, `shippingAddress`, `notes`, `createdAt`, `updatedAt`.

**OrderStatus:** `PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`  

**Order `paymentStatus`:** `PENDING`, `PAID`, `FAILED`, `REFUNDED`

**UpdateOrderStatusRequest (all optional):**

```json
{
  "status": "PROCESSING",
  "paymentStatus": "PAID",
  "paymentId": "optional"
}
```

---

### 5.5 Payments — `/api/payments`

All require **Bearer** (for manual flows; order create already triggers payment).

| Method | Path | Body / query |
|--------|------|--------------|
| POST | `/api/payments/process` | `PaymentRequest` |
| GET | `/api/payments/order/{orderId}` | — |
| GET | `/api/payments/history` | `page`, `size` |
| POST | `/api/payments/{paymentId}/refund` | `{ "reason": "optional" }` |

**PaymentRequest:**

```json
{
  "orderId": "string",
  "amount": 59.98,
  "method": "CREDIT_CARD",
  "description": "optional"
}
```

**`method` enum:** `CREDIT_CARD`, `DEBIT_CARD`, `PAYPAL`, `BANK_TRANSFER`

**Payment service `status`:** `PENDING`, `SUCCESS`, `FAILED`, `REFUNDED`  

Refund only if payment was **SUCCESS** and belongs to the user.

**PaymentResponse (typical):**  
`id`, `orderId`, `userId`, `amount`, `method`, `status`, `transactionId`, `description`, `failureReason`, `createdAt`, `updatedAt`.

---

## 6. Business & UI flows

### 6.1 Onboarding

1. **Register** or **Login** → save `token` + `userId` + profile fields.  
2. Optional: **Validate** token on app load to refresh session.

### 6.2 Browse (guest or logged-in)

1. **List / search / category / detail** — public GET catalog.  
2. Optional: **stock-check** before showing “Add to cart” quantity limits.

### 6.3 Seller (same user, JWT required)

1. **POST** product → becomes seller for that `sellerId`.  
2. **PUT / DELETE** only for **own** products (403 otherwise).

### 6.4 Cart

1. **POST** add item (catalog validated + stock checked server-side).  
2. **GET** cart (optional `?userId=` matching JWT user).  
3. **PUT** quantity; **DELETE** line; **DELETE** `/api/cart` to clear.

### 6.5 Checkout

1. **POST /api/orders** with items + `shippingAddress` (+ optional `notes`).  
2. Show result: order id, `status`, `paymentStatus`, `paymentId` if present.  
3. If payment succeeded, **cart may already be empty** — refresh cart in UI.

### 6.6 After purchase

1. **GET /api/orders** and **GET /api/orders/{id}**.  
2. **GET /api/payments/order/{orderId}** or **GET /api/payments/history**.  
3. **POST refund** only when allowed (success payment, owner).

---

## 7. Pagination (Spring Data)

Paged endpoints return a JSON object similar to:

```json
{
  "content": [ ... ],
  "totalElements": 42,
  "totalPages": 3,
  "size": 20,
  "number": 0,
  "first": true,
  "last": false,
  "sort": { ... }
}
```

Use `page` (0-based), `size`, and `sort` query parameters on:

- `GET /api/catalog/products`  
- `GET /api/catalog/products/search`  
- `GET /api/catalog/products/category/{category}`  
- `GET /api/orders`  
- `GET /api/payments/history`  

---

## 8. Next.js implementation notes

- Use **`NEXT_PUBLIC_API_BASE_URL`** for all `fetch` targets.  
- Attach **`Authorization: Bearer ${token}`** only on protected routes.  
- Handle **401** globally (redirect to login / clear token).  
- Format **dates** from ISO strings; **money** as numbers → `Intl.NumberFormat`.  
- **Do not** send `X-User-Id` from the client for security-sensitive flows; rely on the gateway. Optional `userId` query on **GET cart** must match the JWT user if both are present.

---

## 9. CORS

Gateway `application.yml` currently uses **`allowedOrigins: *`**, so any Vercel URL works without backend edits.

For stricter production, restrict origins to your Vercel production domain (and optionally `*.vercel.app` if supported) in gateway config on Azure — still **no** need to change frontend URL logic beyond env vars; only CORS must align with real browser origins.

---

## 10. Checklist & summary

| Concern | Action |
|---------|--------|
| API base URL | Vercel: `NEXT_PUBLIC_API_BASE_URL` |
| Shop URL | Vercel Domains |
| Token | Store after login; send `Authorization` header |
| Public catalog | GET `/api/catalog/products/**` only |
| Checkout | POST `/api/orders`; handle auto-payment outcome |
| CORS | `*` today; tighten on Azure if needed |

**Checklist**

- [ ] Azure: gateway URL HTTPS  
- [ ] Vercel: `NEXT_PUBLIC_API_BASE_URL` set, redeploy  
- [ ] All client calls use `${BASE}/api/...`  
- [ ] Protected routes send Bearer token  
- [ ] Pagination + error states handled in UI  

---

## 11. Related files in repo

| File | Content |
|------|---------|
| `README.md` | Overview, ports, endpoint table |
| `API_GATEWAY_TESTING.md` | Step-by-step examples & curl (if present) |
| `postman/ecommerce-ms.postman_collection.json` | Import into Postman; set `baseUrl` to Azure gateway for cloud tests |
| `postman/README.md` | Postman import steps |
| `api-gateway/.../JwtAuthenticationFilter.java` | Source of truth for public vs protected paths |

---

*This document combines deployment guidance for **Azure + Vercel** with **system** and **API** details for building the Next.js storefront.*
