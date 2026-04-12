<div align="center">

# StayEase

### Hotel Booking & Reservation Engine

A production-ready backend reservation engine for hotel booking, payment processing,
and platform administration with real-time inventory management.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Stripe](https://img.shields.io/badge/Stripe-Payments-635BFF?style=for-the-badge&logo=stripe&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-Build-02303A?style=for-the-badge&logo=gradle&logoColor=white)

</div>

---

## Table of Contents

- [Overview](#overview)
- [System Design](#system-design)
- [Architecture](#architecture)
- [API Reference](#api-reference)
- [Booking Lifecycle](#booking-lifecycle)
- [Database Schema](#database-schema)
- [Security](#security)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Production Deployment](#production-deployment)
- [Tradeoffs & Limitations](#tradeoffs--limitations)
- [Scaling Strategy](#scaling-strategy)

---

## Overview

StayEase is a backend reservation engine that handles the complete hotel booking lifecycle:

- **Hotel Onboarding** -- Managers register hotels, define room types, and manage inventory
- **Search & Discovery** -- Guests search available hotels by city, dates, and room count with dynamic pricing
- **Booking & Payment** -- Pessimistic inventory locking ensures no overbooking; Stripe handles payments
- **Platform Administration** -- Admins manage users, oversee all hotels, and monitor platform-wide metrics

---

## System Design

### Concurrency & Inventory Locking

Hotel inventory is a high-contention resource. Two users booking the last room at the same time must never both succeed.

StayEase uses **pessimistic write locks** (`SELECT ... FOR UPDATE`) on inventory rows during reservation. This trades throughput for absolute correctness -- no overbooking is possible, even under concurrent load.

### Two-Phase Inventory Model

Bookings follow a reserve-then-confirm cycle:

| Phase | Action | Inventory Effect |
|-------|--------|-----------------|
| **Reserve** | User initiates booking | `reservedCount` incremented, configurable TTL starts |
| **Confirm** | Stripe webhook fires on payment success | `reservedCount` decremented, `bookedCount` incremented |
| **Cancel** | User cancels or TTL expires | Counts reverted, Stripe refund issued if paid |

A **scheduled cleanup job** (60s `fixedDelay`) expires stale reservations with per-booking error isolation -- one failure never blocks the batch.

### Dynamic Pricing Engine

Prices flow through a decorator chain of strategies:

```
Base Price --> Surge Factor --> Occupancy (+20% at >80%) --> Urgency (+15% within 7 days) --> Holiday (+25%)
```

An hourly `@Scheduled` batch job recalculates prices across all hotels (100 per page) and writes results to a precomputed `hotel_price` table for sub-millisecond search queries.

### Webhook-Driven Payments

Payment confirmation relies on Stripe webhooks with **signature verification** -- not client-side redirects. The handler is **idempotent**: duplicate `checkout.session.completed` events are safely ignored.

---

## Architecture

```
                                    +-------------------+
                                    |   Stripe API      |
                                    | (payments/refunds)|
                                    +--------+----------+
                                             |
Client --> CORS --> JWT Filter --> Security Chain --> Controllers --> Services --> Repositories --> PostgreSQL
                        |                                  |
                  Security Headers                   Pricing Engine
              (HSTS, X-Frame, X-Content)          (Decorator Pattern)
```

**Layers:**

| Layer | Responsibility |
|-------|---------------|
| **Controller** | HTTP mapping, request validation, response formatting |
| **Service** | Business rules, authorization checks, transaction boundaries |
| **Repository** | JPA queries, custom JPQL with pessimistic locking |
| **Strategy** | Decorator-pattern pricing engine, decoupled from booking flow |

**Package Layout:**

```
com.bookingsystem/
  config/           ProjectConfig, PasswordEncoderConfig, StripeConfig, WebConfig, OpenApiConfig
  controller/       Admin, Auth, Booking, Hotel, HotelSearch, Room, Inventory, User, Webhook
  dto/              Request/Response DTOs with Jakarta Bean Validation
  entity/           JPA entities (User, Hotel, Room, Booking, Inventory, Guest, HotelPrice)
  entity/enums/     Role, Gender, BookingStatus, PaymentStatus
  exception/        Custom exceptions + GlobalExceptionHandler
  repository/       Spring Data JPA interfaces with custom queries
  security/         SecurityConfig, JWT filter & service, AuthUtils
  service/          Interfaces + impl/ (Admin, Booking, Hotel, Inventory, Room, User)
  strategy/         PricingStrategy chain + PricingUpdateService (scheduled)
```

---

## API Reference

> Base path: **`/api/v1`**
>
> Interactive docs: **`/api/v1/swagger-ui.html`**

### Authentication (`/auth`) -- Public

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/auth/register` | Register new user with `GUEST` role |
| `POST` | `/auth/login` | Returns access token; sets refresh token as HttpOnly cookie |
| `POST` | `/auth/refresh-token` | Issues new access token using refresh cookie |
| `POST` | `/auth/logout` | Clears refresh token cookie |

### User Profile (`/users`) -- Authenticated

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/users/profile` | Get current user's profile |
| `PUT` | `/users/profile` | Update name, gender, or date of birth |
| `PUT` | `/users/change-password` | Change password (requires current password) |

### Hotel Management (`/admin/hotel`) -- HOTEL_MANAGER

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/admin/hotel` | Create hotel (inactive by default) |
| `GET` | `/admin/hotel` | List own hotels |
| `GET` | `/admin/hotel/{id}` | Get hotel details |
| `PUT` | `/admin/hotel/{id}` | Update hotel |
| `PATCH` | `/admin/hotel/{id}/status?status=true` | Activate/deactivate (initializes or clears inventory) |
| `DELETE` | `/admin/hotel/{id}` | Delete hotel, rooms, and inventory |
| `GET` | `/admin/hotel/{id}/bookings` | List hotel's bookings |
| `GET` | `/admin/hotel/{id}/report` | Revenue report (optional `startDate` & `endDate`) |

### Room Management (`/admin/hotels/{hotelId}/rooms`) -- HOTEL_MANAGER

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/admin/hotels/{hotelId}/rooms` | Create room type (auto-generates 1-year inventory if active) |
| `GET` | `/admin/hotels/{hotelId}/rooms` | List all rooms in hotel |
| `GET` | `/admin/hotels/{hotelId}/rooms/{roomId}` | Get room details |
| `PUT` | `/admin/hotels/{hotelId}/rooms/{roomId}` | Update room |
| `DELETE` | `/admin/hotels/{hotelId}/rooms/{roomId}` | Delete room and inventory |

### Inventory Management (`/admin/inventory`) -- HOTEL_MANAGER

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/admin/inventory/rooms/{roomId}` | View room inventory by date |
| `PUT` | `/admin/inventory/rooms/{roomId}` | Bulk update surge factor or close dates |

### Platform Administration (`/admin/platform`) -- ADMIN

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/admin/platform/users?page=0&size=20` | List all users (paginated, max 100/page) |
| `GET` | `/admin/platform/users/{userId}` | Get user details with roles |
| `PUT` | `/admin/platform/users/{userId}/roles` | Update user roles |
| `DELETE` | `/admin/platform/users/{userId}` | Delete user account |
| `GET` | `/admin/platform/hotels?page=0&size=20` | List all hotels (paginated) |
| `PATCH` | `/admin/platform/hotels/{hotelId}/status?active=true` | Activate/deactivate any hotel |
| `GET` | `/admin/platform/bookings?page=0&size=20` | List all bookings (paginated) |
| `GET` | `/admin/platform/stats` | Platform statistics (users, hotels, bookings, revenue) |

### Hotel Search (`/hotels`) -- Public

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/hotels/search` | Search by city, dates, room count (paginated, validated) |
| `GET` | `/hotels/{hotelId}/info` | Hotel details with all room types |

### Bookings (`/bookings`) -- Authenticated

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/bookings/init` | Reserve rooms with pessimistic lock (starts TTL countdown) |
| `POST` | `/bookings/{id}/addguest` | Add guest details to reservation |
| `POST` | `/bookings/{id}/payments` | Create Stripe Checkout session |
| `POST` | `/bookings/{id}/cancel` | Cancel booking (releases inventory; refunds if confirmed) |
| `GET` | `/bookings/{id}/status` | Get current booking status |
| `GET` | `/bookings/my-bookings` | List authenticated user's bookings |

### Webhooks (`/webhooks`) -- Stripe Only

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/webhooks/payment` | Stripe payment confirmation (signature-verified, idempotent) |

---

## Booking Lifecycle

```
                          +--- cancel ---+
                          |              |
RESERVED ──> GUEST_ADDED ──> PAYMENT_PENDING ──> CONFIRMED
    |                                                 |
    +------------ TTL expires ───> CANCELLED <── cancel (+ refund)
```

| Status | Inventory | Trigger |
|--------|-----------|---------|
| `RESERVED` | `reservedCount++` | `POST /bookings/init` |
| `GUEST_ADDED` | No change | `POST /bookings/{id}/addguest` |
| `PAYMENT_PENDING` | No change | `POST /bookings/{id}/payments` |
| `CONFIRMED` | `reservedCount--`, `bookedCount++` | Stripe webhook |
| `CANCELLED` | Counts reverted | User cancel, TTL expiry, or cancel after confirm (+ refund) |

---

## Database Schema

```
users ─────┬──── hotels ──── rooms ──── inventory
            |       |                     (unique: hotel_id + room_id + date)
            |       |                     (pessimistic lock target)
            |       |
            |       └──── hotel_price     (precomputed daily min-price for search)
            |
            └──── bookings ──── guests    (many-to-many)
```

**Key Indexes:**

| Index | Purpose |
|-------|---------|
| `inventory(city, date)` | Hotel search performance |
| `inventory(room_id, date)` | Booking reservation queries |
| `booking(user_id)` | User's booking history |
| `booking(hotel_id)` | Hotel's booking list |
| `booking(status)` | Status-based filtering |
| `booking(paymentSessionId)` | Stripe webhook lookup |

---

## Security

| Layer | Implementation |
|-------|---------------|
| **Authentication** | JWT access tokens (JJWT 0.13, HMAC-SHA) |
| **Token refresh** | HttpOnly + Secure + SameSite=Strict cookie, scoped to `/api/v1/auth` |
| **Password storage** | BCrypt hashing with strength-validated input |
| **Authorization** | Role-based: `GUEST`, `HOTEL_MANAGER`, `ADMIN` |
| **Resource ownership** | Every mutation verifies the requesting user owns the resource |
| **HTTP headers** | X-Frame-Options: DENY, X-Content-Type-Options: nosniff, HSTS (1 year) |
| **CORS** | Configurable allowed origins via `CORS_ALLOWED_ORIGINS` env var |
| **Input validation** | Jakarta Bean Validation on all request DTOs with `@Valid` |
| **Error responses** | Sanitized messages -- no internal IDs or stack traces leak to clients |
| **Webhook security** | Stripe signature verification; invalid signatures return 400 |
| **Actuator** | `/health` is public; `/metrics`, `/info` require `ADMIN` role |
| **Audit trail** | Admin actions logged with `ADMIN_AUDIT:` prefix (user email, action, target) |

---

## Tech Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Java | 21 (LTS) | Language runtime |
| Spring Boot | 4.0.2 | Application framework |
| Spring Security | 7.x | Authentication & authorization |
| Spring Data JPA | 4.x | Data access with JPQL & pessimistic locking |
| PostgreSQL | 15+ | Primary database (ACID, row-level locking, `TEXT[]`) |
| HikariCP | 6.x | Connection pooling (configurable size & timeouts) |
| JJWT | 0.13.0 | Stateless JWT token management |
| Stripe SDK | 31.3.0 | PCI-compliant payments, webhooks, refunds |
| ModelMapper | 3.2.6 | Entity-to-DTO mapping |
| SpringDoc OpenAPI | 2.8.6 | Swagger UI & API documentation |
| Lombok | latest | Boilerplate reduction |
| java-dotenv | 5.2.2 | 12-factor environment configuration |
| Gradle | 9.3 | Build & dependency management |

---

## Getting Started

### Prerequisites

- **Java 21** or later
- **PostgreSQL 15+** running locally or remotely
- **Stripe account** with test API keys ([dashboard.stripe.com](https://dashboard.stripe.com))

### Quick Start

```bash
# Clone the repository
git clone <repo-url> && cd BookingSystem

# Configure environment
cp .env.example .env
# Edit .env with your database credentials, JWT secret, and Stripe keys

# For development, set JPA_DDL_AUTO=update in .env to auto-create tables

# Build and run
./gradlew bootRun

# Verify
curl http://localhost:8080/api/v1/actuator/health
```

**Swagger UI:** [http://localhost:8080/api/v1/swagger-ui.html](http://localhost:8080/api/v1/swagger-ui.html)

---

## Configuration

### Required Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_APPLICATION_NAME` | Application name | `BookingSystem` |
| `DB_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/HotelDB` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `your_password` |
| `JWT_SECRET` | Signing key (min 32 bytes) | Random 64+ char string |
| `JWT_EXPIRATION` | Access token TTL (ms) | `300000` (5 min) |
| `JWT_REFRESH_TOKEN` | Refresh token TTL (ms) | `604800000` (7 days) |
| `STRIPE_KEY` | Stripe secret key | `sk_test_...` |
| `WEBHOOK_SECRET` | Stripe webhook signing secret | `whsec_...` |

### Optional Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JPA_DDL_AUTO` | `validate` | Hibernate schema mode (`update` for dev) |
| `HIKARI_MAX_POOL_SIZE` | `10` | Maximum database connections |
| `HIKARI_MIN_IDLE` | `5` | Minimum idle connections in pool |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Comma-separated allowed origins |
| `FRONTEND_URL` | `http://localhost:8080` | Stripe payment redirect base URL |
| `BOOKING_TTL_MINUTES` | `10` | Reservation expiry time in minutes |
| `STRIPE_CURRENCY` | `inr` | ISO 4217 currency code for payments |
| `STRIPE_MIN_AMOUNT` | `5000` | Minimum charge in smallest currency unit |
| `LOG_LEVEL` | `INFO` | Application log level (`DEBUG`, `INFO`, `WARN`) |

---

## Production Deployment

| Area | Recommendation |
|------|---------------|
| **Schema management** | Set `JPA_DDL_AUTO=validate`. Use [Flyway](https://flywaydb.org/) or [Liquibase](https://www.liquibase.com/) for migrations. |
| **Connection pool** | Tune `HIKARI_MAX_POOL_SIZE` based on `max_connections / instance_count`. |
| **HTTPS** | Terminate TLS at the reverse proxy (nginx, AWS ALB). Refresh cookies are marked `Secure`. |
| **CORS** | Set `CORS_ALLOWED_ORIGINS` to your production frontend domain(s). |
| **Monitoring** | Integrate `/actuator/metrics` with Prometheus + Grafana. |
| **Stripe webhooks** | Register your public URL in Stripe dashboard; update `WEBHOOK_SECRET`. |
| **Logging** | Route structured logs to a centralized system (ELK, CloudWatch, Datadog). |
| **Secrets** | Use a secrets manager (Vault, AWS Secrets Manager) instead of `.env` files. |

---

## Tradeoffs & Limitations

### Pessimistic vs Optimistic Locking

Pessimistic locks hold database row locks for the duration of the transaction. Under extreme concurrency (thousands of simultaneous bookings for the same room), this causes lock contention and potential timeouts. For hotel-level concurrency (not airline-level), pessimistic locking provides the strongest correctness guarantee with acceptable throughput.

### Hourly Price Recalculation

Dynamic prices update on an hourly schedule, not in real-time. Bookings between recalculations use the latest price snapshot. This keeps the booking path fast and avoids recalculating prices under lock contention.

### Single-Region Deployment

The pessimistic locking strategy requires a single PostgreSQL writer, limiting geographic scaling. Read replicas can serve search and read-heavy queries.

### Known Limitations

| Feature | Status |
|---------|--------|
| Email notifications | Not implemented |
| Redis caching (search, hotel details) | Not implemented |
| Image upload (S3 integration) | Not implemented |
| Holiday calendar for pricing | Date-based heuristic only |
| Rate limiting (auth, search) | Recommended via API gateway or Resilience4j |
| JWT refresh token server-side revocation | Cookie cleared on logout, token valid until expiry |

---

## Scaling Strategy

```
Phase 1 (Vertical)          Phase 2 (Horizontal)           Phase 3 (Distributed)
─────────────────           ────────────────────           ──────────────────────
HikariCP tuning             PostgreSQL read replicas       Message queue (Kafka/RabbitMQ)
Query optimization          Redis caching layer            Event-driven payment flow
Index tuning                API gateway + rate limiting    Distributed tracing (Zipkin)
                            Multiple app instances         Multi-region read replicas
```

---

<div align="center">

**Built with Spring Boot 4 | Java 21 | PostgreSQL | Stripe**

</div>
