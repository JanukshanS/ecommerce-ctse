# E-Commerce Microservices Platform

A microservice-based e-commerce application built with Spring Boot and Spring Cloud, containerized with Docker, and deployed on Azure Container Apps.

## Architecture

The application is composed of 6 independently deployable microservices:

| Service | Port | Description |
|---------|------|-------------|
| **API Gateway** | 8080 | Entry point for all client requests. Handles JWT validation, request routing, circuit breaking, and retry logic. |
| **Auth Service** | 8081 | User registration, login, and JWT token management. |
| **Catalog Service** | 8082 | Product catalog management — CRUD, search, category browsing, and stock checking. |
| **Cart Service** | 8083 | Shopping cart management. Validates products and stock against the Catalog Service. |
| **Order Service** | 8084 | Order creation and management. Triggers payment processing and clears cart on success. |
| **Payment Service** | 8085 | Payment processing and refunds. Updates order status after payment resolution. |

### Inter-Service Communication

```
Client → API Gateway (JWT validation) → Individual Services

Cart Service    ──→  Catalog Service  (product validation, stock check)
Order Service   ──→  Payment Service  (trigger payment on order creation)
Order Service   ──→  Cart Service     (clear cart after successful order)
Payment Service ──→  Order Service    (update order status after payment)
```

All external traffic flows through the API Gateway, which validates JWT tokens and forwards `X-User-Id` / `X-Username` headers to downstream services. Services also communicate directly with each other for business workflows.

## Tech Stack

- **Language:** Java 17
- **Framework:** Spring Boot 3.2.3, Spring Cloud 2023.0.1
- **Gateway:** Spring Cloud Gateway with Resilience4j (circuit breaker, retry)
- **Database:** MongoDB 7.0 (one database per service)
- **Auth:** JWT (jjwt 0.11.5) with BCrypt password hashing
- **Containerization:** Docker (multi-stage builds)
- **CI/CD:** GitHub Actions (build, test, SonarCloud SAST, Docker push)
- **SAST:** SonarCloud + JaCoCo code coverage
- **Container Registry:** Docker Hub
- **Cloud:** Azure Container Apps / MongoDB Atlas

## Prerequisites

- Java 17 (Temurin)
- Maven 3.9+
- Docker & Docker Compose

## Running Locally

1. Clone the repository:
   ```bash
   git clone https://github.com/JanukshanS/ecommerce-ctse.git
   cd ecommerce-ctse
   ```

2. Create the environment file:
   ```bash
   cp .env.example .env
   # Edit .env with your MongoDB URI and JWT secret
   ```

3. Start all services:
   ```bash
   docker compose up --build
   ```

4. Access the application:
   - API Gateway: http://localhost:8080
   - Swagger UI (per service): http://localhost:{port}/swagger-ui.html

## API Endpoints

All endpoints are accessible through the API Gateway at `http://localhost:8080`.

### Auth (`/api/auth`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/register` | Register a new user |
| POST | `/login` | Login and receive JWT token |
| GET | `/validate` | Validate a JWT token |

### Catalog (`/api/catalog/products`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create a product |
| GET | `/` | List all active products |
| GET | `/{id}` | Get product by ID |
| GET | `/category/{category}` | Browse by category |
| GET | `/search?q=` | Search products |
| PUT | `/{id}` | Update a product |
| DELETE | `/{id}` | Soft delete a product |
| GET | `/{id}/stock-check?quantity=` | Check stock availability |

### Cart (`/api/cart`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | Get current user's cart |
| POST | `/items` | Add item to cart |
| PUT | `/items/{productId}` | Update item quantity |
| DELETE | `/items/{productId}` | Remove item from cart |
| DELETE | `/` | Clear entire cart |

### Orders (`/api/orders`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Create an order (triggers payment) |
| GET | `/` | List user's orders |
| GET | `/{id}` | Get order by ID |
| PUT | `/{id}/status` | Update order status |
| DELETE | `/{id}` | Cancel an order |

### Payments (`/api/payments`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/process` | Process a payment |
| GET | `/order/{orderId}` | Get payment by order ID |
| GET | `/history` | Get payment history |
| POST | `/{paymentId}/refund` | Refund a payment |

## CI/CD Pipeline

Each service has its own GitHub Actions workflow that triggers on changes to the respective service directory:

1. **Build & Test** — Compiles and runs unit tests with Maven
2. **SAST Analysis** — SonarCloud static analysis with JaCoCo coverage
3. **Docker Build & Push** — Builds container image and pushes to Docker Hub

## Security

- JWT-based authentication with shared signing key
- BCrypt password hashing for stored credentials
- API Gateway validates tokens and propagates user context
- Spring Security configured per service
- SonarCloud SAST integrated in CI pipeline
- JaCoCo code coverage reporting
- Stateless session management across all services

## Project Structure

```
ecommerce-ctse/
├── api-gateway/          # Spring Cloud Gateway
├── auth-service/         # Authentication microservice
├── catalog-service/      # Product catalog microservice
├── cart-service/         # Shopping cart microservice
├── order-service/        # Order management microservice
├── payment-service/      # Payment processing microservice
├── postman/              # Postman collection for API testing
├── .github/workflows/    # CI/CD pipeline configurations
├── docker-compose.yml    # Local development orchestration
├── pom.xml               # Parent Maven POM
└── .env.example          # Environment variables template
```

## Running Tests

```bash
# Run all tests
mvn clean verify

# Run tests for a specific service
mvn clean verify -pl cart-service -am
```

## Team

| Member | Microservice |
|--------|-------------|
| Janukshan | Auth Service |
| Janukshan | Catalog Service |
| Samudith | Cart Service |
| Asath | Order Service |
| Dilnuk | Payment Service |

