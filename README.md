# Flash Sale Engine

A high-concurrency flash sale backend built using Java, Spring Boot, PostgreSQL, and Redis.

---

# Tech Stack

- Java 21
- Spring Boot
- PostgreSQL
- Redis
- Docker
- Maven
- JPA / Hibernate

---

# Features

- User registration and management
- Product creation and inventory management
- Redis-based inventory caching
- Atomic inventory decrement
- Overselling prevention
- Duplicate purchase prevention
- Idempotent purchase requests
- Distributed rate limiting
- Global exception handling

---

# Architecture

```text
Client
   ↓
Spring Boot API
   ↓
Redis
   ├── Inventory
   ├── Rate Limiting
   ├── Idempotency
   └── Purchase Locks
   ↓
PostgreSQL
   ├── Users
   ├── Products
   └── Orders
```

---

# Project Structure

```text
src/main/java/com/flashsale
│
├── config
├── controller
├── dto
├── entity
├── exception
├── repository
└── service
```

---

# Prerequisites

- Java 21
- Docker Desktop
- Maven
- PostgreSQL
- Redis

---

# Running the Project

## Start Infrastructure

```bash
docker compose up -d
```

## Run Application

```bash
mvn spring-boot:run
```

Application runs on:

```text
http://localhost:8080
```

---

# API Endpoints

## Users

### Create User

```http
POST /users
```

Example:

```json
{
  "name": "Alice",
  "email": "alice@test.com"
}
```

---

## Products

### Create Product

```http
POST /products
```

Example:

```json
{
  "name": "PS5",
  "description": "Gaming Console",
  "price": 499.99,
  "inventory": 5
}
```

---

## Purchase

### Purchase Product

```http
POST /purchase
```

Example:

```json
{
  "userId": "USER_ID",
  "productId": "PRODUCT_ID",
  "idempotencyKey": "purchase-123"
}
```

---
