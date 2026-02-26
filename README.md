
# 🏨 StayEase — Hotel Booking & Reservation Engine

**A production-grade, enterprise-ready hotel booking platform built with Spring Boot 4, featuring real-time inventory management, dynamic pricing strategies, Stripe payment integration, and JWT-based security.**

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.2-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Stripe](https://img.shields.io/badge/Stripe-Payments-635BFF?style=for-the-badge&logo=stripe&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-Auth-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)

</div>

---

## 📑 Table of Contents

- [Project Overview](#-project-overview)
- [Features](#-features)
- [System Design Philosophy](#-system-design-philosophy)
- [Architecture Overview](#-architecture-overview)
- [Tech Stack](#-tech-stack)
- [Folder Structure](#-folder-structure)
- [Data Flow](#-data-flow)
- [Database Design](#-database-design)
- [Security](#-security)
- [API Documentation](#-api-documentation)
- [Performance & Scalability](#-performance--scalability)
- [DevOps & Deployment](#-devops--deployment)
- [Testing Strategy](#-testing-strategy)
- [Monitoring & Logging](#-monitoring--logging)
- [Future Improvements](#-future-improvements)
- [Contribution Guide](#-contribution-guide)
- [Setup & Installation](#-setup--installation)
- [Environment Variables](#-environment-variables)
- [Developer Guide](#-developer-guide)
- [Business & Scaling Vision](#-business--scaling-vision)

---

## 🚀 Project Overview

### The Problem
The hospitality industry needs reliable, scalable booking systems that handle real-time room availability, dynamic pricing, concurrent reservations, and secure payment processing — without overbooking or data races.

### What StayEase Solves
StayEase is a **backend reservation engine** that manages the full lifecycle of hotel bookings — from hotel/room onboarding by managers, to guest search, reservation with pessimistic inventory locking, multi-strategy dynamic pricing, Stripe checkout, webhook-driven payment confirmation, and automated refunds on cancellation.

### Target Users
| Role | Description |
|------|-------------|
| **Guests** | Search hotels by city/dates, book rooms, manage bookings |
| **Hotel Managers** | Onboard hotels, manage rooms, activate/deactivate listings, view booking reports |
| **Platform Operators** | Monitor system health via Actuator, manage pricing strategies |

### Business Value
- **Zero overbooking** via pessimistic database locking on inventory
- **Revenue optimization** via multi-layered dynamic pricing (surge, occupancy, urgency, holiday)
- **Secure payments** via Stripe Checkout with webhook-driven confirmation
- **Scalable architecture** with clean layered design ready for microservice extraction

---

## 🎯 Features

### Functional Features
- ✅ User registration & login with JWT access + refresh tokens
- ✅ Hotel CRUD with ownership validation (only owner can modify)
- ✅ Room management tied to hotels with automatic inventory generation
- ✅ Hotel search by city, dates, and room count with paginated results
- ✅ Multi-step booking: Reserve → Add Guests → Pay → Confirm
- ✅ 5-minute reservation expiry window to prevent inventory hoarding
- ✅ Stripe Checkout payment with success/failure redirects
- ✅ Stripe Webhook for server-side payment confirmation
- ✅ Booking cancellation with automated Stripe refund
- ✅ Guest management per booking

### Technical Capabilities
- 🔒 Pessimistic write locking on inventory for concurrency safety
- 💰 Decorator-pattern dynamic pricing engine (4 chained strategies)
- ⏰ Scheduled hourly price recalculation via Spring `@Scheduled`
- 🍪 Refresh token stored in HttpOnly secure cookie (XSS-resistant)
- 🛡️ Role-based access control (`GUEST`, `HOTEL_MANAGER`)
- 📊 Spring Actuator health/metrics/info endpoints
- 🔄 Transactional inventory management with reserve/confirm/cancel flows

---

## 🧠 System Design Philosophy

### Architecture Principles
| Principle | Implementation |
|-----------|----------------|
| **Layered Architecture** | Controller → Service → Repository with strict dependency direction |
| **Interface Segregation** | Every service has an interface; implementations are injectable |
| **Decorator Pattern** | Pricing strategies are chained decorators (Base → Surge → Occupancy → Urgency → Holiday) |
| **DDD Lite** | Domain entities encapsulate business rules; enums model state machines |
| **Fail-Fast** | Custom exceptions with `@RestControllerAdvice` global handler |
| **Separation of Concerns** | Auth logic isolated in `security` package; payment logic in `TransactionalService` |

### Key Design Decisions

**1. Pessimistic Locking over Optimistic Locking**
> Hotel inventory is a high-contention resource. Pessimistic `WRITE` locks on inventory rows prevent double-booking at the database level — accepting slightly lower throughput for absolute data correctness.

**2. Decorator Pattern for Pricing**
> Rather than a monolithic pricing calculator with conditional branches, each pricing strategy wraps the previous one. This makes it trivial to add/remove/reorder pricing rules without touching existing code (Open/Closed Principle).

**3. Webhook-Driven Payment Confirmation**
> Instead of polling Stripe for payment status, the system uses a webhook endpoint with Stripe signature verification. This ensures reliable, event-driven payment confirmation even if the user closes their browser.

**4. Two-Phase Inventory (Reserve → Confirm)**
> Bookings first *reserve* inventory (decrementing available count), then upon payment confirmation *confirm* it (moving from reserved to booked). Cancellation reverses the booked count. This prevents ghost bookings from holding inventory indefinitely.

---

## 🏗️ Architecture Overview

### High-Level System Design

```
┌──────────────┐     ┌───────────────────────────────────────────────────────┐
│   Frontend   │────▶│                  API Gateway (/api/v1)                │
│  (React/Any) │◀────│                                                       │
└──────────────┘     └───────┬───────────────────────────────────┬───────────┘
                             │                                   │
                    ┌────────▼────────┐                ┌────────▼────────┐
                    │  Auth Module    │                │  Booking Module  │
                    │  ┌────────────┐ │                │  ┌────────────┐ │
                    │  │ JWT Filter │ │                │  │ Controller │ │
                    │  │ JWT Service│ │                │  │ Service    │ │
                    │  │ Auth Svc   │ │                │  │ Repository │ │
                    │  └────────────┘ │                │  └────────────┘ │
                    └────────┬────────┘                └────────┬────────┘
                             │                                   │
         ┌───────────────────┼───────────────────────────────────┤
         │                   │                                   │
┌────────▼────────┐ ┌───────▼────────┐  ┌───────────────┐ ┌────▼──────────┐
│  Hotel Module   │ │ Inventory Mgmt │  │ Pricing Engine│ │ Stripe Payment│
│  ┌────────────┐ │ │ ┌────────────┐ │  │ ┌───────────┐ │ │ ┌───────────┐ │
│  │ Hotels     │ │ │ │ Pessimistic│ │  │ │ Base      │ │ │ │ Checkout  │ │
│  │ Rooms      │ │ │ │ Locking    │ │  │ │ Surge     │ │ │ │ Webhook   │ │
│  │ Activation │ │ │ │ Reserve    │ │  │ │ Occupancy │ │ │ │ Refund    │ │
│  └────────────┘ │ │ │ Confirm    │ │  │ │ Urgency   │ │ │ └───────────┘ │
└─────────────────┘ │ │ Cancel     │ │  │ │ Holiday   │ │ └───────────────┘
                    │ └────────────┘ │  │ └───────────┘ │
                    └───────┬────────┘  └───────┬───────┘
                            │                   │
                    ┌───────▼───────────────────▼───────┐
                    │         PostgreSQL Database        │
                    │  users | hotels | rooms | booking  │
                    │  inventory | guests | hotel_price  │
                    └───────────────────────────────────┘
```

### Request Flow
```
Client Request → CORS Filter → JWT Auth Filter → Security Chain
    → Controller → Service (Business Logic) → Repository (JPA/JPQL)
    → PostgreSQL → Response DTO → JSON Response
```

---

## ⚙️ Tech Stack

| Layer | Technology | Why This Choice |
|-------|-----------|-----------------|
| **Runtime** | Java 21 | LTS with virtual threads, pattern matching, sealed classes |
| **Framework** | Spring Boot 4.0.2 | Latest Spring Boot with best-in-class DI, auto-config, and ecosystem |
| **ORM** | Spring Data JPA + Hibernate | Declarative repositories, JPQL for complex queries, pessimistic locking |
| **Database** | PostgreSQL | ACID compliance, `TEXT[]` arrays for amenities/photos, robust locking |
| **Auth** | Spring Security + JJWT 0.13 | Stateless JWT auth with industry-standard HMAC-SHA signing |
| **Payments** | Stripe Java SDK 31.3 | PCI-compliant payment processing, webhook support, refund API |
| **Validation** | Jakarta Validation | Annotation-driven request validation (`@Valid`, `@NotBlank`, `@Email`) |
| **Mapping** | ModelMapper 3.2.6 | Automatic DTO↔Entity conversion reducing boilerplate |
| **Config** | java-dotenv 5.2.2 | 12-factor app env variable management |
| **Build** | Gradle (Groovy DSL) | Fast incremental builds, dependency management |
| **Monitoring** | Spring Actuator | Health checks, metrics, info endpoints for observability |
| **Boilerplate** | Lombok | `@Getter`, `@Setter`, `@Builder`, `@RequiredArgsConstructor` |

---

## 📂 Folder Structure

```
src/main/java/com/bookingsystem/
├── BookingSystemApplication.java    # Application entry point
├── config/
│   ├── ProjectConfig.java           # Bean definitions (ModelMapper)
│   ├── StripeConfig.java            # Stripe API key initialization
│   └── WebConfig.java               # CORS configuration
├── controller/
│   ├── AuthController.java          # Register, Login, Refresh Token
│   ├── BookingController.java       # Init, Add Guests, Pay, Cancel, Status
│   ├── HotelController.java         # CRUD for hotel managers (/admin/hotel)
│   ├── HotelSearchController.java   # Public hotel search (/hotels)
│   ├── RoomController.java          # Room CRUD per hotel (/admin/hotels/{id}/rooms)
│   └── WebhookController.java       # Stripe payment webhook (/webhooks/payment)
├── dto/                             # 22 Request/Response DTOs
│   ├── BookingRequest/Response.java
│   ├── HotelRequest/Response.java
│   ├── LoginRequest/Response.java
│   ├── RegisterRequest.java
│   ├── HotelSearchRequest/Response.java
│   └── ...
├── entity/
│   ├── Booking.java                 # Core booking with state machine
│   ├── Guest.java                   # Guest info per booking
│   ├── Hotel.java                   # Hotel with embedded contact, photos, amenities
│   ├── HotelContact.java           # @Embeddable contact (address, phone, email, location)
│   ├── HotelPrice.java             # Precomputed daily hotel min-price
│   ├── Inventory.java              # Per-room per-date availability with surge factor
│   ├── Room.java                   # Room type, price, capacity, photos
│   ├── User.java                   # Implements UserDetails for Spring Security
│   └── enums/                      # BookingStatus, Gender, PaymentStatus, Role
├── exception/
│   ├── GlobalExceptionHandler.java  # @RestControllerAdvice — centralized error handling
│   ├── ResourceNotFoundException.java
│   ├── RoomNotAvailableException.java
│   ├── BookingExpiredException.java
│   ├── UnAuthorisedException.java
│   └── ...                          # 11 custom exception classes
├── repository/                      # 7 JPA repositories
│   ├── InventoryRepository.java     # Complex JPQL with pessimistic locking
│   ├── HotelPriceRepository.java    # Aggregation queries for search
│   └── ...
├── security/
│   ├── SecurityConfig.java          # Filter chain, RBAC rules, CORS, CSRF
│   ├── jwt/
│   │   ├── JwtAuthFilter.java       # OncePerRequestFilter for token extraction
│   │   └── JwtService.java          # Token generation, validation, parsing
│   └── service/
│       ├── AuthService.java
│       └── impl/AuthServiceImpl.java
├── service/
│   ├── BookingService.java          # + impl with full booking lifecycle
│   ├── HotelService.java           # + impl with ownership validation
│   ├── InventoryService.java       # + impl with year-long inventory init
│   ├── RoomService.java            # + impl with auto-inventory on create
│   ├── TransactionalService.java   # + impl for Stripe Checkout session
│   └── UserService.java           # + impl with UserDetailsService
└── strategy/
    ├── PricingStrategy.java         # Interface (Decorator pattern)
    ├── PricingService.java          # Chains all strategies
    ├── PricingUpdateService.java    # @Scheduled hourly batch price update
    └── impl/
        ├── BasePricingStrategy.java      # Room base price
        ├── SurgePricingStrategy.java     # Multiplies by surge factor
        ├── OccupancyPricingStrategy.java # +20% when >80% occupancy
        ├── UrgencyPricingStrategy.java   # +15% for bookings within 7 days
        └── HolidayPricingStrategy.java   # +25% on holidays (stub)
```

---

## 🔄 Data Flow

### Booking Lifecycle (State Machine)

```
    ┌──────────┐  POST /bookings/init   ┌─────────────┐
    │  START   │───────────────────────▶│  RESERVED   │
    └──────────┘                        └──────┬──────┘
                                               │ POST /bookings/{id}/addguest
                                        ┌──────▼──────┐
                                        │ GUEST_ADDED │
                                        └──────┬──────┘
                                               │ POST /bookings/{id}/payments
                                       ┌───────▼────────┐
                                       │PAYMENT_PENDING │
                                       └───────┬────────┘
            ┌──────────────────────────────────┼──────────────────┐
            │ Stripe Webhook (success)         │                  │ Timeout/Failure
     ┌──────▼──────┐                           │           ┌──────▼──────┐
     │  CONFIRMED  │                           │           │  EXPIRED    │
     └──────┬──────┘                           │           └─────────────┘
            │ POST /bookings/{id}/cancel       │
     ┌──────▼──────┐                           │
     │  CANCELLED  │ (+ Stripe Refund)         │
     └─────────────┘                           │
```

### Authentication Flow
1. **Register** → `POST /auth/register` → BCrypt hash → Save User with `GUEST` role
2. **Login** → `POST /auth/login` → AuthenticationManager validates → Generate access token (5 min) + refresh token (7 days in HttpOnly cookie)
3. **Authenticated Request** → `Authorization: Bearer <token>` → `JwtAuthFilter` extracts userId → Loads User → Sets SecurityContext
4. **Token Refresh** → `POST /auth/refresh-token` → Reads cookie → Validates refresh token → Issues new access token

---

## 🧱 Database Design

### Entity Relationship Diagram

```
┌──────────┐     ┌──────────┐     ┌───────────┐
│  users   │────▶│  hotels  │────▶│   rooms   │
│──────────│  1:N│──────────│  1:N│───────────│
│ id (PK)  │     │ id (PK)  │     │ id (PK)   │
│ name     │     │ name     │     │ hotel_id  │
│ email    │     │ city     │     │ type      │
│ password │     │ photos[] │     │ basePrice │
│ roles    │     │ amenities│     │ capacity  │
└──────────┘     │ owner_id │     │ totalCount│
                 │ active   │     └─────┬─────┘
                 └──────────┘           │ 1:N
                      │ 1:N      ┌──────▼──────┐
                 ┌────▼─────┐    │  inventory  │
                 │ booking  │    │─────────────│
                 │──────────│    │ hotel_id    │
                 │ hotel_id │    │ room_id     │
                 │ room_id  │    │ date        │
                 │ user_id  │    │ bookedCount │
                 │ checkIn  │    │ reservedCnt │
                 │ checkOut │    │ totalCount  │
                 │ amount   │    │ surgeFactor │
                 │ status   │    │ price       │
                 │ sessionId│    │ city        │
                 └────┬─────┘    │ closed      │
                      │ M:N     └─────────────┘
                 ┌────▼─────┐    UNIQUE(hotel_id, room_id, date)
                 │  guests  │
                 │──────────│
                 │ name     │
                 │ gender   │
                 │ dob      │
                 └──────────┘
```

### Key Design Choices
- **Inventory table with unique constraint** `(hotel_id, room_id, date)` — one row per room-type per day
- **Pessimistic write locks** on inventory queries prevent concurrent overbooking
- **`TEXT[]` PostgreSQL arrays** for photos/amenities — avoids extra join tables
- **`HotelPrice` precomputed table** — daily minimum prices per hotel for fast search queries
- **Two-counter system**: `reservedCount` (pending payment) + `bookedCount` (confirmed) enable precise availability tracking

---

## 🔐 Security

| Aspect | Implementation |
|--------|---------------|
| **Authentication** | JWT (JJWT 0.13) with HMAC-SHA signing |
| **Access Token** | 5-minute expiry, sent via `Authorization: Bearer` header |
| **Refresh Token** | 7-day expiry, stored in `HttpOnly` + `Secure` cookie (XSS-proof) |
| **Password Storage** | BCrypt hashing via `BCryptPasswordEncoder` |
| **RBAC** | `GUEST` and `HOTEL_MANAGER` roles; `/admin/**` restricted to managers |
| **CORS** | Whitelisted origins (`localhost:3000`, `localhost:5173`) |
| **CSRF** | Disabled (stateless JWT-based API) |
| **Session** | `STATELESS` — no server-side session storage |
| **Webhook Security** | Stripe signature verification via `Webhook.constructEvent()` |
| **Auth Entry Point** | Custom `AuthEntryPointJwt` for 401 responses |
| **Ownership Validation** | Every hotel/booking mutation verifies requesting user is the owner |

---

## 📡 API Documentation

### Base URL: `/api/v1`

### Authentication
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/auth/register` | Public | Register new user |
| `POST` | `/auth/login` | Public | Login, returns access token + refresh cookie |
| `POST` | `/auth/refresh-token` | Cookie | Refresh access token |

### Hotel Management (HOTEL_MANAGER only)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/admin/hotel` | Create hotel |
| `GET` | `/admin/hotel` | List all hotels |
| `GET` | `/admin/hotel/{id}` | Get hotel by ID |
| `PUT` | `/admin/hotel/{id}` | Update hotel |
| `PATCH` | `/admin/hotel/{id}/status?status=true` | Activate/deactivate |
| `DELETE` | `/admin/hotel/{id}` | Delete hotel + rooms + inventory |

### Room Management (HOTEL_MANAGER only)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/admin/hotels/{hotelId}/rooms` | Create room (auto-generates 1yr inventory if hotel active) |
| `GET` | `/admin/hotels/{hotelId}/rooms` | List rooms in hotel |
| `GET` | `/admin/hotels/{hotelId}/rooms/{roomId}` | Get room |
| `DELETE` | `/admin/hotels/{hotelId}/rooms/{roomId}` | Delete room + inventory |

### Hotel Search (Public)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/hotels/search` | Search available hotels (paginated) |
| `GET` | `/hotels/{hotelId}/info` | Get hotel details with rooms |

### Bookings (Authenticated)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/bookings/init` | Initialize booking (reserves inventory) |
| `POST` | `/bookings/{id}/addguest` | Add guest details |
| `POST` | `/bookings/{id}/payments` | Initiate Stripe Checkout |
| `POST` | `/bookings/{id}/cancel` | Cancel + refund |
| `GET` | `/bookings/{id}/status` | Check booking status |

### Webhooks (Stripe)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/webhooks/payment` | Stripe payment confirmation |

### Sample Request/Response

**POST `/auth/register`**
```json
// Request
{ "name": "John Doe", "email": "john@example.com", "password": "SecureP@ss123" }
// Response 200
{ "message": "User registered successfully!" }
```

**POST `/bookings/init`**
```json
// Request
{ "hotelId": 1, "roomId": 3, "checkInDate": "2026-03-15", "checkOutDate": "2026-03-18", "roomsCount": 2 }
// Response 200
{ "id": 42, "hotelId": 1, "roomId": 3, "checkInDate": "2026-03-15",
  "checkOutDate": "2026-03-18", "roomsCount": 2, "amount": 15600.00, "status": "RESERVED" }
```

---

## ⚡ Performance & Scalability

| Strategy | Implementation |
|----------|---------------|
| **Concurrency Control** | `@Lock(PESSIMISTIC_WRITE)` on inventory queries prevents race conditions |
| **Batch Processing** | `PricingUpdateService` processes hotels in pages of 100 to avoid OOM |
| **Pagination** | Hotel search uses `PageRequest` for cursor-based result sets |
| **Lazy Loading** | `FetchType.LAZY` on all `@ManyToOne` relations to prevent N+1 queries |
| **Precomputed Prices** | `HotelPrice` table stores daily min-prices — search queries avoid real-time computation |
| **Scheduled Updates** | Hourly cron (`0 0 * * * *`) recalculates dynamic prices in background |
| **Stateless Auth** | No server session storage; horizontal scaling without sticky sessions |
| **Bulk Saves** | `saveAll()` for inventory initialization and price updates |

### Scaling Roadmap
- **Read Replicas** → Route search queries to PostgreSQL replicas
- **Redis Caching** → Cache hotel search results and hotel info
- **Message Queue** → Decouple payment confirmation via Kafka/RabbitMQ
- **Connection Pooling** → HikariCP tuning for high-concurrency workloads

---

## 🛠️ DevOps & Deployment

### Environment Configuration
```
.env (local)          → Environment-specific variables
application.yaml      → Spring config with ${VAR} placeholders
```

### Containerization (Recommended)
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/BookingSystem-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Monitoring
- **Health Check**: `GET /api/v1/actuator/health` (show-details: always)
- **Metrics**: `GET /api/v1/actuator/metrics`
- **Info**: `GET /api/v1/actuator/info`

---

## 🧪 Testing Strategy

| Type | Tool | Status |
|------|------|--------|
| **Unit Tests** | JUnit 5 + Spring Test | Foundation in place |
| **Security Tests** | Spring Security Test | Available via dependency |
| **Integration Tests** | Spring Boot Test | `BookingSystemApplicationTests` configured |

### Recommended Additions
- **Testcontainers** for PostgreSQL integration tests
- **Mockito** for service layer unit tests
- **REST Assured** for API contract testing
- **JMeter/Gatling** for load testing the booking flow under concurrency

---

## 📊 Monitoring & Logging

| Component | Details |
|-----------|---------|
| **Health Endpoint** | `/actuator/health` with `show-details: always` |
| **Metrics** | `/actuator/metrics` — JVM, HTTP, Hikari pool stats |
| **Logging** | Spring Boot default (Logback) — `application.yaml` configurable |

### Recommended Observability Stack
- **ELK Stack** (Elasticsearch + Logstash + Kibana) for centralized log aggregation
- **Prometheus + Grafana** for metrics dashboards (Actuator metrics are Prometheus-compatible)
- **Sentry** for exception tracking and alerting

---

## 🚧 Future Improvements

| Priority | Improvement | Impact |
|----------|-------------|--------|
| 🔴 High | Implement `getMyBookings()` and `getHotelReport()` (currently stubs) | User-facing feature |
| 🔴 High | Implement `updateProfile()` in UserService | User profile management |
| 🔴 High | Holiday calendar integration in `HolidayPricingStrategy` | Revenue optimization |
| 🟡 Medium | Add Redis caching for hotel search and room info | Latency reduction |
| 🟡 Medium | Implement email notifications (booking confirmation, cancellation) | User experience |
| 🟡 Medium | Add rate limiting on auth endpoints | Security hardening |
| 🟡 Medium | Switch `ddl-auto` from `create-drop` to `validate` + Flyway migrations | Production-readiness |
| 🟢 Low | Add Swagger/OpenAPI documentation (`springdoc-openapi`) | Developer experience |
| 🟢 Low | Implement image upload for hotel/room photos (S3 integration) | Feature completeness |
| 🟢 Low | Multi-currency support in Stripe (currently INR only) | International expansion |

---

## 🤝 Contribution Guide

### Branch Strategy
```
main        → Production-ready code
develop     → Integration branch
feature/*   → Feature branches (e.g., feature/redis-caching)
hotfix/*    → Critical production fixes
```

### PR Rules
1. All PRs must target `develop` branch
2. Minimum 1 code review approval required
3. All existing tests must pass
4. New features must include unit tests
5. Follow existing package structure and naming conventions

### Coding Standards
- **Naming**: `PascalCase` for classes, `camelCase` for methods/variables
- **DTOs**: Separate Request/Response DTOs — never expose entities directly
- **Services**: Always code to interfaces (`XService` + `XServiceImpl`)
- **Validation**: Use Jakarta validation annotations on DTOs
- **Exceptions**: Create custom exceptions; handle in `GlobalExceptionHandler`

---

## 🧾 Setup & Installation

### Prerequisites
- **Java 21** (JDK)
- **PostgreSQL 15+**
- **Stripe Account** (test keys)
- **Gradle 8+** (or use included `gradlew`)

### Local Setup

```bash
# 1. Clone the repository
git clone https://github.com/your-username/BookingSystem.git
cd BookingSystem

# 2. Create PostgreSQL database
psql -U postgres -c "CREATE DATABASE HotelDB;"

# 3. Configure environment variables
cp .env.example .env
# Edit .env with your database credentials, JWT secret, and Stripe keys

# 4. Build and run
./gradlew bootRun

# 5. Verify
curl http://localhost:8080/api/v1/actuator/health
```

### Production Setup
```bash
# Build JAR
./gradlew clean build -x test

# Run with production profile
java -jar build/libs/BookingSystem-0.0.1-SNAPSHOT.jar
```

---

## 🌍 Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_APPLICATION_NAME` | Application name for Spring | `BookingSystem` |
| `DB_URL` | PostgreSQL JDBC connection URL | `jdbc:postgresql://localhost:5432/HotelDB` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `your_password` |
| `JWT_SECRET` | HMAC-SHA signing key (min 64 chars) | `<long-random-string>` |
| `JWT_EXPIRATION` | Access token TTL in milliseconds | `300000` (5 min) |
| `JWT_REFRESH_TOKEN` | Refresh token TTL in milliseconds | `604800000` (7 days) |
| `STRIPE_KEY` | Stripe secret API key | `sk_test_...` |
| `WEBHOOK_SECRET` | Stripe webhook endpoint secret | `whsec_...` |

> ⚠️ **Never commit `.env` to version control.** It is already in `.gitignore`.

---

## 📘 Developer Guide

### Adding a New Feature
1. Create entity in `entity/` package with JPA annotations
2. Create DTO(s) in `dto/` package for request/response
3. Create repository interface extending `JpaRepository` in `repository/`
4. Create service interface in `service/` and implementation in `service/impl/`
5. Create controller in `controller/` with appropriate `@RequestMapping`
6. Add security rules in `SecurityConfig` if needed
7. Create custom exception(s) and add handler in `GlobalExceptionHandler`

### Adding a New Pricing Strategy
1. Create class implementing `PricingStrategy` in `strategy/impl/`
2. Accept `PricingStrategy` in constructor (decorator pattern)
3. Call `pricingStrategy.calculatePrice(inventory)` then apply your logic
4. Chain it in `PricingService.calculateDynamicPricing()`

### Debugging Tips
- Enable SQL logging: add `spring.jpa.show-sql: true` in `application.yaml`
- Check Actuator health: `GET /api/v1/actuator/health`
- Validate JWT tokens at [jwt.io](https://jwt.io)
- Use Stripe CLI for local webhook testing: `stripe listen --forward-to localhost:8080/api/v1/webhooks/payment`

---

## 📈 Business & Scaling Vision

### Growth Path
| Phase | Description |
|-------|-------------|
| **Phase 1** (Current) | Single-hotel-manager platform with core booking + payments |
| **Phase 2** | Multi-tenant marketplace with manager dashboards and analytics |
| **Phase 3** | Mobile apps (React Native), push notifications, loyalty programs |
| **Phase 4** | AI-powered pricing recommendations, demand forecasting, review system |

### Monetization
- **Commission Model**: Platform takes 10-15% per confirmed booking
- **Premium Listings**: Featured hotel placement in search results
- **SaaS Model**: Subscription-based hotel management dashboard
- **Dynamic Pricing Add-on**: Advanced pricing analytics as paid tier

### Enterprise Readiness
- ✅ Role-based access control
- ✅ API versioning (`/api/v1`)
- ✅ Stateless architecture (horizontal scaling ready)
- ✅ Payment processing with refund support
- 🔄 Needs: Audit logging, multi-region DB, rate limiting, API gateway

---


**Built with ❤️ using Spring Boot 4 · Java 21 · PostgreSQL · Stripe**

⭐ Star this repository if you find it useful!

