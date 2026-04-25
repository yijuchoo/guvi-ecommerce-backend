# E-Commerce Backend System

A production-like scalable backend system for an e-commerce platform built with Java Spring Boot and MongoDB.

---

## Project Overview

This project implements a fully functional e-commerce backend with user authentication, product catalog management, shopping cart, order processing, and payment simulation. It is designed with real-world concerns in mind including JWT-based security, concurrent stock management, idempotent operations, and role-based access control.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.0.5 |
| Database | MongoDB Atlas |
| Authentication | JWT (JSON Web Tokens) via JJWT 0.12.6 |
| Authorization | Spring Security |
| API Documentation | Springdoc OpenAPI (Swagger UI) |
| Build Tool | Maven |
| Deployment | Render (Docker) |
| Testing | JUnit 5 + Mockito |

---

## Setup Instructions

### Prerequisites

Make sure you have the following installed before starting:

- Java 17 — https://adoptium.net
- IntelliJ IDEA Community — https://www.jetbrains.com/idea/download
- Git — https://git-scm.com/downloads
- Postman — https://www.postman.com/downloads
- Maven (bundled with IntelliJ)

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/ecommerce-backend.git
cd ecommerce-backend
```

### 2. Set Up MongoDB Atlas

1. Go to https://cloud.mongodb.com and create a free account
2. Create a free M0 cluster
3. Under **Security → Database Access**, create a new user:
   - Choose any username and password
   - Role: **Read and write to any database**
4. Under **Security → Network Access**, click **Add IP Address → Allow Access from Anywhere** (`0.0.0.0/0`)
5. Click **Connect → Drivers** and copy the connection string
6. Add `/ecommerce?` before the query parameters:
```
mongodb+srv://<username>:<yourpassword>@cluster0.xxxxx.mongodb.net/ecommerce?retryWrites=true&w=majority
```

### 3. Configure application.properties

Open `src/main/resources/application.properties` and update with your values:

```properties
spring.application.name=ecommerce

# MongoDB
spring.data.mongodb.uri=mongodb+srv://<username>:<yourpassword>@cluster0.xxxxx.mongodb.net/ecommerce?
retryWrites=true&w=majority

# JWT
jwt.secret=
jwt.expiration=86400000

# Swagger UI
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.operationsSorter=method
```

> **Important:** Never commit this file to GitHub. It is listed in `.gitignore`.

### 4. Build the Project

In IntelliJ, open the Maven panel on the right side → Lifecycle → double-click **clean** then **package**. Or run in terminal:

```bash
mvn clean package -DskipTests
```

### 5. Run the Application

Open `EcommerceApplication.java` in IntelliJ and click the green **Run** button. The app starts on port 8080.

### 6. Access Swagger UI

Open your browser and go to:
```
http://localhost:8080/swagger-ui.html
```

### 7. Create an Admin User

1. Register a user via `POST /auth/register`
2. Go to MongoDB Atlas → Browse Collections → users
3. Find the user document and change `roles` from `["ROLE_USER"]` to `["ROLE_ADMIN"]`
4. Login again via `POST /auth/login` to get a fresh admin token

---

## Environment Variables

For production deployment the following environment variables must be set:

| Variable | Description |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Set to `prod` to load `application-prod.properties` |
| `MONGO_URI` | Full MongoDB Atlas connection string |
| `JWT_SECRET` | Secret key for signing JWT tokens (min 64 characters) |

---

## API Documentation

### Base URL

- Local: `http://localhost:8080`
- Production: `https://guvi-ecommerce-backend.onrender.com`

### Authentication

All protected endpoints require a Bearer token in the Authorization header:
```
Authorization: Bearer <your_jwt_token>
```

---

### Auth Endpoints

#### POST /auth/register
Register a new user.

**Request Body:**
```json
{
  "email": "user@test.com",
  "password": "123456"
}
```

**Response (201):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "user@test.com",
  "role": "ROLE_USER"
}
```

---

#### POST /auth/login
Login and receive a JWT token.

**Request Body:**
```json
{
  "email": "user@test.com",
  "password": "123456"
}
```

**Response (200):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "user@test.com",
  "role": "ROLE_USER"
}
```

---

### Product Endpoints

#### GET /products
Get all products with pagination and optional search.

**Query Parameters:**

| Parameter | Default | Description |
|---|---|---|
| `page` | `0` | Page number |
| `size` | `10` | Items per page |
| `sortBy` | `name` | Field to sort by |
| `keyword` | - | Search by name or category |

**Sample Request:**
```
GET /products?page=0&size=10&sortBy=name
GET /products?keyword=laptop&page=0&size=10
```

**Response (200):**
```json
{
  "content": [
    {
      "id": "675abc123",
      "name": "Laptop Pro",
      "description": "High performance laptop",
      "price": 1299.99,
      "stockQuantity": 50,
      "category": "Electronics"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "size": 10,
  "number": 0
}
```

---

#### GET /products/{id}
Get a single product by ID.

**Response (200):**
```json
{
  "id": "675abc123",
  "name": "Laptop Pro",
  "price": 1299.99,
  "stockQuantity": 50,
  "category": "Electronics"
}
```

---

#### POST /products *(Admin only)*
Create a new product.

**Headers:** `Authorization: Bearer <admin_token>`

**Request Body:**
```json
{
  "name": "Laptop Pro",
  "description": "High performance laptop",
  "price": 1299.99,
  "stockQuantity": 50,
  "category": "Electronics"
}
```

**Response (201):** Returns the created product object.

---

#### PUT /products/{id} *(Admin only)*
Update an existing product.

**Headers:** `Authorization: Bearer <admin_token>`

**Request Body:** Same as POST /products

**Response (200):** Returns the updated product object.

---

#### DELETE /products/{id} *(Admin only)*
Delete a product.

**Headers:** `Authorization: Bearer <admin_token>`

**Response (204):** No content.

---

### Cart Endpoints

All cart endpoints require authentication.

**Headers:** `Authorization: Bearer <user_token>`

#### GET /cart
View the current user's cart.

**Response (200):**
```json
{
  "id": "cart123",
  "userId": "user1",
  "items": [
    {
      "productId": "675abc123",
      "productName": "Laptop Pro",
      "quantity": 2,
      "price": 1299.99
    }
  ]
}
```

---

#### POST /cart/items
Add an item to the cart.

**Request Body:**
```json
{
  "productId": "675abc123",
  "quantity": 2
}
```

**Response (200):** Returns the updated cart.

---

#### PUT /cart/items/{productId}
Update item quantity in cart.

**Query Parameter:** `quantity=5`

**Sample Request:**
```
PUT /cart/items/675abc123?quantity=5
```

**Response (200):** Returns the updated cart.

---

#### DELETE /cart/items/{productId}
Remove an item from the cart.

**Response (200):** Returns the cart with the item removed.

---

### Order Endpoints

All order endpoints require authentication.

**Headers:** `Authorization: Bearer <user_token>`

#### POST /orders
Place an order from the current cart.

**Headers:**
```
Authorization: Bearer <user_token>
Idempotency-Key: order-001
```

> The `Idempotency-Key` header is required. Use any unique string. Sending the same key twice returns the existing order instead of creating a duplicate.

**Response (201):**
```json
{
  "id": "order123",
  "userId": "user1",
  "items": [
    {
      "productId": "675abc123",
      "productName": "Laptop Pro",
      "quantity": 2,
      "priceAtPurchase": 1299.99
    }
  ],
  "totalAmount": 2599.98,
  "status": "PLACED",
  "idempotencyKey": "order-001",
  "createdAt": "2024-01-15T10:30:00"
}
```

---

#### GET /orders
Get all orders for the current user.

**Response (200):** Array of order objects.

---

#### GET /orders/{id}
Get a specific order by ID.

**Response (200):** Returns the order object.

---

### Payment Endpoints

#### POST /payments
Process payment for an order.

**Headers:** `Authorization: Bearer <user_token>`

**Request Body:**
```json
{
  "orderId": "order123"
}
```

**Response (200) — Success:**
```json
{
  "id": "pay123",
  "orderId": "order123",
  "userId": "user1",
  "amount": 2599.98,
  "status": "SUCCESS",
  "processedAt": "2024-01-15T10:31:00"
}
```

**Response (200) — Failed:**
```json
{
  "id": "pay124",
  "orderId": "order123",
  "status": "FAILED",
  "amount": 2599.98
}
```

> Payment simulation uses an 80% success / 20% failure random outcome. On success the order status changes to `CONFIRMED`. On failure the order status changes to `CANCELLED`.

---

### Error Responses

| Status Code | Meaning |
|---|---|
| 400 | Bad request — validation failed |
| 401 | Unauthorized — missing or invalid token |
| 403 | Forbidden — insufficient role (e.g. non-admin accessing admin endpoint) |
| 404 | Resource not found |
| 500 | Internal server error |

**Error Response Format:**
```json
{
  "error": "Product not found: 675abc123"
}
```

---

## Design Decisions & Tradeoffs

### Concurrency & Oversell Prevention

Stock deduction uses MongoDB's `findAndModify` — a single atomic operation that reads and deducts stock in one step. The query includes a stock availability condition (`stockQuantity >= requested`), meaning concurrent requests for the same item are safely handled without application-level locks. If two users simultaneously attempt to buy the last item, only one succeeds — the other receives an `InsufficientStockException`.

Additionally the `Product` model uses Spring Data MongoDB's `@Version` annotation for optimistic locking as a secondary safety net. If two threads read the same product document and both attempt to save, the second save fails with an `OptimisticLockingFailureException` due to the version mismatch.

**Tradeoff:** The current implementation deducts stock per item in a loop. If deduction succeeds for item 1 and 2 but fails for item 3, items 1 and 2 are already deducted with no automatic rollback. A production system would implement compensating transactions. For this project scope the atomic per-item deduction is sufficient.

### Idempotency

**Order placement** accepts a client-provided `Idempotency-Key` header. Before creating a new order the system checks if an order with that key already exists and returns it if found. The key is also enforced as a unique MongoDB index, preventing duplicates even under race conditions.

**Payment processing** checks for an existing payment record for the order before processing. Calling `POST /payments` twice for the same order returns an error on the second call rather than charging twice.

### JWT Authentication

Stateless JWT authentication is used instead of sessions. Each token contains the user's email and role, signed with HMAC-SHA256. Tokens expire after 24 hours. The role is embedded in the token so the server does not need a database lookup on every request.

**Tradeoff:** Because tokens are stateless, revoking a specific token before expiry is not straightforward. A production system would maintain a token blacklist or use short-lived access tokens with refresh tokens.

### Embedded Documents vs References

`CartItem` and `OrderItem` are embedded directly inside their parent documents rather than stored as separate collections. This decision is based on:

- Cart items have no meaning outside their parent cart
- Order items must snapshot the price at purchase time — embedding ensures historical accuracy even if the product price changes later
- Single query fetches the entire cart or order with all items — no joins needed

### Password Storage

All passwords are hashed using BCrypt before storage. The plain text password is never stored or logged anywhere.

---

## Running Unit Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn -Dtest=ProductServiceTest test

# Run all tests and generate coverage report
mvn test jacoco:report
```

Test reports are generated at `target/site/jacoco/index.html`.

### Test Coverage

| Class | Tests |
|---|---|
| `ProductServiceTest` | Get by ID, create, update, delete, not found cases |
| `CartServiceTest` | Add item, insufficient stock, product not found, remove item, new cart creation |
| `OrderServiceTest` | Place order, idempotency key, empty cart, insufficient stock, get orders |

---

## Deployment

### Live URLs

| Resource                 | URL                                                               |
|--------------------------|-------------------------------------------------------------------|
| **Backend Deployed URL** | https://guvi-ecommerce-backend.onrender.com                       |
| **Backend API**          | https://xxxx.onrender.com                                         |
| **Swagger Document URL** | https://guvi-ecommerce-backend.onrender.com/swagger-ui/index.html |
| **Swagger UI**           | https://xxxx.onrender.com/swagger-ui/index.html                   |

> Replace `xxxx` with your actual Render service identifier.

> **Note:** This project is deployed on Render's free tier. The service spins down after 15 minutes of inactivity. The first request after an idle period may take 30–60 seconds to respond. Subsequent requests are fast.

### Deployment Platform

The application is deployed on **Render** using Docker.

### Deployment Steps

1. Push code to GitHub (secrets are excluded via `.gitignore`)
2. Connect GitHub repository to Render
3. Set Runtime to **Docker**
4. Set the following environment variables in Render dashboard:

| Key | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `MONGO_URI` | Your MongoDB Atlas connection string |
| `JWT_SECRET` | Your JWT secret key |

5. Render automatically builds and deploys on every push to `main`

---

## Project Structure

```
src/main/java/com/yourname/ecommerce/
├── config/
│   ├── MongoConfig.java
│   ├── SecurityConfig.java
│   └── SwaggerConfig.java
├── controller/
│   ├── AuthController.java
│   ├── CartController.java
│   ├── OrderController.java
│   ├── PaymentController.java
│   └── ProductController.java
├── dto/
│   ├── AuthResponse.java
│   ├── CartItemRequest.java
│   ├── LoginRequest.java
│   ├── PaymentRequest.java
│   ├── ProductRequest.java
│   └── RegisterRequest.java
├── exception/
│   ├── GlobalExceptionHandler.java
│   ├── InsufficientStockException.java
│   └── ResourceNotFoundException.java
├── model/
│   ├── Cart.java
│   ├── CartItem.java
│   ├── Order.java
│   ├── OrderItem.java
│   ├── Payment.java
│   ├── Product.java
│   └── User.java
├── repository/
│   ├── CartRepository.java
│   ├── OrderRepository.java
│   ├── PaymentRepository.java
│   ├── ProductRepository.java
│   └── UserRepository.java
├── security/
│   ├── JwtFilter.java
│   ├── JwtUtil.java
│   └── UserDetailsServiceImpl.java
└── service/
    ├── CartService.java
    ├── OrderService.java
    ├── PaymentService.java
    ├── ProductService.java
    └── UserService.java
```

---

## Submission Checklist

- [x] GitHub repository is complete and clean
- [x] README is well-documented
- [x] Backend is deployed and working on Render
- [x] Swagger UI is accessible at `/swagger-ui.html`
- [x] APIs are testable via Postman and Swagger
- [x] No secrets exposed in repository
- [x] Core features implemented: Auth, Products, Cart, Orders, Payments
- [x] Unit tests written with JUnit 5 and Mockito
- [x] Concurrency handled with atomic MongoDB operations
- [x] Idempotency implemented for orders and payments
