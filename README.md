# StayEase - Hotel Booking System

A production-grade hotel reservation engine built with Spring Boot 4.0.2 and Java 21. Features real-time inventory management with pessimistic locking, decorator-based dynamic pricing, Stripe payment integration, JWT authentication, and enterprise observability.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Entity Relationship Model](#entity-relationship-model)
- [Database Schema](#database-schema)
- [Authentication & Authorization](#authentication--authorization)
- [Booking Lifecycle](#booking-lifecycle)
- [Dynamic Pricing Engine](#dynamic-pricing-engine)
- [Inventory Management](#inventory-management)
- [Stripe Payment Integration](#stripe-payment-integration)
- [API Reference](#api-reference)
- [Rate Limiting](#rate-limiting)
- [Caching Strategy](#caching-strategy)
- [Observability](#observability)
- [Error Handling](#error-handling)
- [Security](#security)
- [Getting Started](#getting-started)
- [Configuration Reference](#configuration-reference)
- [Testing](#testing)

---

## Architecture Overview

StayEase follows a layered monolithic architecture with clear separation of concerns, designed for an eventual transition to a modular monolith or microservices.

```
                             +-------------------+
                             |   Web / Mobile    |
                             |     Client        |
                             +--------+----------+
                                      |
                                      | HTTPS + JWT Bearer
                                      v
                        +-------------+-------------+
                        |     API Gateway Layer      |
                        |  (Context Path: /api/v1)   |
                        +---+---+---+---+---+---+----+
                            |   |   |   |   |   |
           +----------------+   |   |   |   |   +----------------+
           |                    |   |   |   |                    |
           v                    v   |   v   v                    v
    +------+------+    +-------+-+  | +-+-------+       +--------+------+
    |    Auth     |    | Hotel   |  | | Booking |       |    Admin      |
    | Controller  |    | Search  |  | | Control |       |  Controller   |
    +------+------+    +-------+-+  | +-+-------+       +--------+------+
           |                   |    |   |                        |
           v                   v    v   v                        v
    +------+------+    +-------+----+---+-------+       +--------+------+
    |  Auth       |    | Hotel  | Room  |Booking|       |    Admin      |
    |  Service    |    |Service |Service|Service|       |   Service     |
    +------+------+    +---+----+---+---+---+---+       +--------+------+
           |               |        |       |                    |
           v               v        v       v                    v
    +------+------+    +---+--------+-------+---+       +--------+------+
    |  JWT        |    |    Inventory Service    |       |    Audit      |
    |  Service    |    +--------+----------------+       |   Service     |
    +------+------+            |                        +--------+------+
           |                   |                                 |
           v                   v                                 v
    +------+-------------------+--+    +-----------+    +--------+------+
    |       PostgreSQL            |    |  Stripe   |    | Async Thread  |
    |   (Pessimistic Locks)       |    |  Gateway  |    |    Pool       |
    +-----------------------------+    +-----------+    +---------------+
```

### Key Architectural Decisions

**1. Pessimistic Locking for Inventory Correctness**

The worst-case scenario in hospitality is overbooking. The system uses `SELECT ... FOR UPDATE` (pessimistic write locks) on inventory rows during booking initialization. This guarantees zero overbooking at the cost of slightly reduced throughput compared to optimistic locking or Redis-based counters.

**2. Decorator Pattern for Pricing**

Hotel pricing varies by base price, surge factor, occupancy, urgency, and holidays. The pricing engine uses the decorator pattern to compose pricing strategies into a chain. New pricing rules can be added without modifying existing code.

**3. Two-Phase Booking with TTL**

Bookings follow a two-phase commit pattern: inventory is first reserved (with a configurable TTL, default 10 minutes), then confirmed upon successful payment. A scheduled cleanup job releases expired reservations every 60 seconds.

**4. Stateless JWT Authentication**

No server-side sessions. Access tokens are short-lived and carried as Bearer tokens. Refresh tokens are stored in HttpOnly, Secure, SameSite=Strict cookies to prevent XSS and CSRF attacks.

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Framework | Spring Boot | 4.0.2 |
| Language | Java | 21 |
| Database | PostgreSQL | 16+ |
| ORM | Hibernate / Spring Data JPA | - |
| Connection Pool | HikariCP | - |
| Auth | JWT (jjwt) | 0.13.0 |
| Payments | Stripe | 31.3.0 |
| Caching | Caffeine | - |
| Rate Limiting | Resilience4j | 2.3.0 |
| Metrics | Micrometer + Prometheus | - |
| API Docs | SpringDoc OpenAPI (Swagger) | 2.8.6 |
| Object Mapping | ModelMapper | 3.2.6 |
| Build | Gradle | 9+ |
| Testing | JUnit 5, Mockito, Spring Security Test | - |

---

## Project Structure

```
src/main/java/com/bookingsystem/
|
+-- BookingSystemApplication.java          # Entry point, loads .env, enables scheduling
|
+-- config/
|   +-- AsyncConfig.java                   # Thread pool for async pricing updates & audit
|   +-- CacheConfig.java                   # Caffeine cache definitions (search, info, stats)
|   +-- CorrelationIdFilter.java           # X-Correlation-ID MDC filter for request tracing
|   +-- OpenApiConfig.java                 # Swagger/OpenAPI configuration
|   +-- PasswordEncoderConfig.java         # BCrypt encoder (isolated to avoid circular deps)
|   +-- ProjectConfig.java                 # ModelMapper bean
|   +-- StripeConfig.java                  # Stripe API key initialization
|   +-- WebConfig.java                     # CORS configuration
|
+-- controller/
|   +-- AdminController.java               # Platform admin endpoints (/admin/platform/**)
|   +-- AuthController.java                # Register, login, logout, refresh token
|   +-- BookingController.java             # Booking lifecycle endpoints
|   +-- HotelController.java               # Hotel CRUD for managers (/admin/hotel/**)
|   +-- HotelSearchController.java         # Public hotel search (/hotels/**)
|   +-- InventoryController.java           # Inventory management (/admin/inventory/**)
|   +-- RoomController.java                # Room CRUD (/admin/hotels/{id}/rooms/**)
|   +-- UserController.java                # User profile management
|   +-- WebhookController.java             # Stripe webhook receiver
|
+-- dto/                                   # Request/Response DTOs with Jakarta validation
|   +-- ApiResponse.java                   # Generic API response wrapper with correlation ID
|   +-- BookingRequest.java                # Booking creation payload
|   +-- BookingResponse.java               # Booking details response
|   +-- BookingPaymentInitResponse.java    # Stripe session URL response
|   +-- BookingStatusResponse.java
|   +-- HotelRequest.java                  # Hotel creation/update payload
|   +-- HotelResponse.java
|   +-- HotelSearchRequest.java            # Search query parameters
|   +-- HotelSearchResponse.java
|   +-- HotelPriceDto.java                 # Internal projection for search results
|   +-- HotelPriceResponse.java
|   +-- HotelInfoDto.java                  # Hotel details with room list
|   +-- HotelReport.java                   # Revenue report DTO
|   +-- InventoryRequest.java              # Inventory update payload
|   +-- InventoryResponse.java
|   +-- RoomRequest.java                   # Room creation/update payload
|   +-- RoomResponse.java
|   +-- RegisterRequest.java               # User registration with validation
|   +-- LoginRequest.java
|   +-- LoginResponse.java                 # Access + refresh tokens
|   +-- AuthResponse.java
|   +-- UserDto.java
|   +-- AdminUserDto.java                  # User view for admin (includes roles)
|   +-- ProfileUpdateRequest.java
|   +-- ChangePasswordRequest.java
|   +-- GuestDto.java                      # Guest details for booking
|   +-- PlatformStatsDto.java              # Admin dashboard stats
|   +-- UpdateUserRolesRequest.java
|
+-- entity/
|   +-- User.java                          # Implements UserDetails, multi-role support
|   +-- Hotel.java                         # Hotel with embedded contact, owner relation
|   +-- Room.java                          # Room type within a hotel
|   +-- Booking.java                       # Booking with state machine, guest list
|   +-- Inventory.java                     # Per-room per-date availability record
|   +-- Guest.java                         # Guest details for a booking
|   +-- HotelContact.java                  # Embeddable contact info
|   +-- HotelPrice.java                    # Pre-computed daily min price per hotel
|   +-- AuditLog.java                      # Admin action audit trail
|   +-- enums/
|       +-- Role.java                      # GUEST, HOTEL_MANAGER, ADMIN
|       +-- BookingStatus.java             # RESERVED, GUEST_ADDED, PAYMENT_PENDING, CONFIRMED, CANCELLED
|       +-- Gender.java                    # MALE, FEMALE, OTHER
|       +-- PaymentStatus.java             # PENDING, CONFIRMED, CANCELLED
|
+-- repository/                            # Spring Data JPA repositories
|   +-- UserRepository.java
|   +-- HotelRepository.java
|   +-- RoomRepository.java
|   +-- BookingRepository.java             # Custom queries: expired bookings, revenue sum
|   +-- InventoryRepository.java           # Pessimistic lock queries, atomic updates
|   +-- GuestRepository.java
|   +-- HotelPriceRepository.java          # Search with availability + avg price
|   +-- AuditLogRepository.java
|
+-- security/
|   +-- SecurityConfig.java                # Filter chain, endpoint authorization rules
|   +-- jwt/
|   |   +-- JwtAuthFilter.java             # OncePerRequestFilter, extracts Bearer token
|   |   +-- JwtService.java                # Token generation, validation, parsing
|   +-- service/
|   |   +-- AuthService.java               # Auth interface
|   |   +-- impl/AuthServiceImpl.java       # Register, login, refresh token logic
|   +-- utils/
|       +-- AuthUtils.java                 # Static getCurrentUser() helper
|
+-- service/
|   +-- AdminService.java
|   +-- AuditService.java
|   +-- BookingService.java
|   +-- HotelService.java
|   +-- InventoryService.java
|   +-- RoomService.java
|   +-- TransactionalService.java          # Stripe checkout session creation
|   +-- UserService.java
|   +-- impl/                              # All implementations
|       +-- AdminServiceImpl.java          # User/hotel/booking management + stats
|       +-- AuditServiceImpl.java          # Async audit logging with IP resolution
|       +-- BookingCleanupService.java     # Scheduled expired booking cleanup
|       +-- BookingServiceImpl.java        # Full booking lifecycle
|       +-- HotelServiceImpl.java          # Hotel CRUD with cache eviction
|       +-- InventoryServiceImpl.java      # Inventory init, search, updates
|       +-- RoomServiceImpl.java           # Room CRUD with auto-inventory
|       +-- TransactionalServiceImpl.java  # Stripe session management
|       +-- UserServiceImpl.java           # UserDetailsService + profile management
|
+-- strategy/                              # Dynamic pricing engine
|   +-- PricingStrategy.java               # Strategy interface
|   +-- PricingService.java                # Builds decorator chain, calculates prices
|   +-- PricingUpdateService.java          # Hourly scheduled price recalculation
|   +-- HotelPricingUpdateWorker.java      # Async per-hotel price update
|   +-- impl/
|       +-- BasePricingStrategy.java       # Returns room base price
|       +-- SurgePricingStrategy.java      # Multiplies by surge factor
|       +-- OccupancyPricingStrategy.java  # +20% when occupancy > 80%
|       +-- UrgencyPricingStrategy.java    # +15% for bookings within 7 days
|       +-- HolidayPricingStrategy.java    # +25% on holidays (Dec 24/25/31, Jan 1)
|
+-- exception/
    +-- GlobalExceptionHandler.java        # @RestControllerAdvice, structured error responses
    +-- AuthEntryPointJwt.java             # 401 JSON response for unauthenticated requests
    +-- APIException.java                  # Generic 400 Bad Request
    +-- AccessDeniedException.java
    +-- BookingExpiredException.java        # 410 Gone
    +-- InvalidBookingStateException.java   # 409 Conflict
    +-- ResourceNotFoundException.java      # 404 Not Found
    +-- RoomNotAvailableException.java      # 409 Conflict
    +-- UnAuthorisedException.java          # 403 Forbidden
    +-- UserAlreadyExistsException.java     # 409 Conflict
    +-- UserNotFoundException.java          # 404 Not Found
```

---

## Entity Relationship Model

```
+----------+       +----------+       +-----------+       +-------------+
|  User    |1-----*| Booking  |*-----1|   Room    |*-----1|   Hotel     |
|----------|       |----------|       |-----------|       |-------------|
| id (PK)  |       | id (PK)  |       | id (PK)   |       | id (PK)     |
| name     |       | hotel_id |       | hotel_id   |       | name        |
| email(U) |       | room_id  |       | type       |       | city        |
| password |       | user_id  |       | basePrice  |       | photos[]    |
| gender   |       | roomsCnt |       | photos[]   |       | amenities[] |
| dob      |       | checkIn  |       | amenities[]|       | active      |
| roles[]  |       | checkOut |       | totalCount |       | owner_id    |
+----+-----+       | amount   |       | capacity   |       | contact     |
     |             | status   |       +-----------+       +------+------+
     |             | session  |                                  |
     |             +-----+----+                                  |
     |                   |                                       |
     |              *----+----*                                  |
     |             | Booking  |                           1------+------*
     |             | _Guest   |                           |  Inventory  |
     |             +----+-----+                           |-------------|
     |                  |                                 | id (PK)     |
     |             *----+----*                            | hotel_id    |
     |             |  Guest   |                           | room_id     |
     |             |----------|                           | date        |
     +--------1----|  user_id |                           | bookedCount |
                   |  name    |                           | reservedCnt |
                   |  gender  |                           | totalCount  |
                   |  dob     |                           | surgeFactor |
                   +----------+                           | price       |
                                                          | city        |
+----------+                                              | closed      |
|AuditLog  |       +------------+                         +-------------+
|----------|       | HotelPrice |
| id (PK)  |       |------------|          +------------+
| action   |       | id (PK)    |          | user_roles |
| actorMail|       | hotel_id   |          |------------|
| targetEnt|       | date       |          | user_id    |
| targetId |       | price      |          | role       |
| details  |       +------------+          +------------+
| ipAddress|
| createdAt|
+----------+
```

### Entity Details

**User** - Implements Spring Security `UserDetails`. Supports multiple roles via `@ElementCollection`. Email is used as the username for authentication. Passwords are BCrypt-encoded.

**Hotel** - Owned by a User (HOTEL_MANAGER). Contains embedded `HotelContact` (address, phone, email, location). Starts as inactive; activation triggers inventory generation for all rooms. Database indexes on `(city, active)` and `owner_id`.

**Room** - Belongs to a Hotel. Defines a room type (e.g., "Deluxe", "Suite") with base price, capacity, total count, photos, and amenities.

**Booking** - Links User, Hotel, and Room. Tracks the number of rooms booked, date range, total amount, and payment session ID. Has a state machine (see [Booking Lifecycle](#booking-lifecycle)). Database indexes on `user_id`, `hotel_id`, `status`, `paymentSessionId`, `(status, createdAt)`, and `(hotel_id, createdAt)`.

**Inventory** - One record per room per date. Tracks `totalCount`, `bookedCount`, `reservedCount`, `surgeFactor`, `price`, and `closed` status. Unique constraint on `(hotel_id, room_id, date)`. The critical table for availability and concurrency control.

**Guest** - Guest details attached to a booking. Linked to the booking via a join table `booking_guest`.

**HotelPrice** - Pre-computed daily minimum price across all room types for a hotel. Used by the search query to return average prices without scanning full inventory. Updated hourly by the pricing update job.

**AuditLog** - Records admin actions (role changes, user deletions, hotel status changes) with actor email, IP address, target entity, and details. Written asynchronously to avoid blocking the main request.

---

## Database Schema

### Key Constraints and Indexes

| Table | Constraint/Index | Purpose |
|-------|-----------------|---------|
| `inventory` | `UNIQUE(hotel_id, room_id, date)` | Prevents duplicate inventory rows |
| `inventory` | `INDEX(city, date)` | Optimizes hotel search by city + date range |
| `inventory` | `INDEX(room_id, date)` | Optimizes room-level availability checks |
| `booking` | `INDEX(user_id)` | Fast lookup of user's bookings |
| `booking` | `INDEX(hotel_id)` | Fast lookup of hotel's bookings |
| `booking` | `INDEX(status)` | Cleanup job filters by status |
| `booking` | `INDEX(paymentSessionId)` | Webhook lookup by Stripe session |
| `booking` | `INDEX(status, createdAt)` | Expired booking cleanup query |
| `booking` | `INDEX(hotel_id, createdAt)` | Hotel report date-range queries |
| `hotel` | `INDEX(city, active)` | Search filters active hotels by city |
| `hotel` | `INDEX(owner_id)` | Manager's hotel list |
| `hotel_price` | `INDEX(hotel_id, date)` | Search query joins on hotel + date range |
| `audit_logs` | `INDEX(action)`, `INDEX(actorEmail)`, `INDEX(createdAt)`, `INDEX(targetEntity, targetId)` | Audit trail lookups |
| `users` | `UNIQUE(email)` | Unique login identity |

### Hibernate Optimizations

```yaml
hibernate:
  jdbc.batch_size: 50      # Batch inserts (inventory initialization)
  order_inserts: true       # Group INSERT statements for batching
  order_updates: true       # Group UPDATE statements for batching
```

`open-in-view: false` is set to prevent lazy loading outside transactions.

---

## Authentication & Authorization

### JWT Token Flow

```
1. POST /auth/register     -->  Creates user with GUEST role
2. POST /auth/login        -->  Returns access_token (body) + refreshToken (HttpOnly cookie)
3. Authenticated requests  -->  Authorization: Bearer <access_token>
4. POST /auth/refresh-token -->  Reads refreshToken cookie, returns new access_token
5. POST /auth/logout       -->  Clears refreshToken cookie (maxAge=0)
```

### Token Details

| Token | Storage | Lifetime | Content |
|-------|---------|----------|---------|
| Access Token | Client memory (response body) | Configurable via `JWT_EXPIRATION` | `sub`: user ID, `roles`: user roles |
| Refresh Token | HttpOnly Secure cookie | Configurable via `JWT_REFRESH_TOKEN` | `sub`: user ID |

The refresh token cookie is scoped to path `/api/v1/auth` with `SameSite=Strict` and `Secure=true`.

### Role-Based Access Control

| Role | Prefix | Access |
|------|--------|--------|
| `GUEST` | `ROLE_GUEST` | Book hotels, manage own bookings, update profile |
| `HOTEL_MANAGER` | `ROLE_HOTEL_MANAGER` | All of GUEST + manage hotels, rooms, inventory, view reports |
| `ADMIN` | `ROLE_ADMIN` | All of above + platform-wide user/hotel/booking management, view stats |

### Security Filter Chain Rules

```
/admin/platform/**     -->  ADMIN role required
/admin/**              -->  HOTEL_MANAGER role required
/bookings/**           -->  Authenticated (any role)
/users/**              -->  Authenticated (any role)
/actuator/health       -->  Public
/actuator/**           -->  ADMIN role required
/swagger-ui/**, /api-docs/**  -->  Public
Everything else        -->  Public (auth, hotels/search, webhooks)
```

### Security Headers

- `X-Frame-Options: DENY` - Prevents clickjacking
- `X-Content-Type-Options: nosniff` - Prevents MIME type sniffing
- `Strict-Transport-Security: max-age=31536000; includeSubDomains` - Enforces HTTPS

---

## Booking Lifecycle

### State Machine

```
    +----------+       +-----------+       +-----------------+       +-----------+
    | RESERVED | ----> |GUEST_ADDED| ----> | PAYMENT_PENDING | ----> | CONFIRMED |
    +----+-----+       +-----+-----+       +--------+--------+       +-----------+
         |                   |                      |
         |                   |                      |
         +--------+----------+----------+-----------+
                  |                     |
                  v                     v
            +-----------+         +-----------+
            | CANCELLED |         | CANCELLED |
            | (expired) |         |  (user)   |
            +-----------+         +-----------+
```

### State Transitions

| From | To | Trigger | Action |
|------|----|---------|--------|
| - | `RESERVED` | `POST /bookings/init` | Validates dates, pessimistic-locks inventory, increments `reservedCount`, calculates dynamic price |
| `RESERVED` | `GUEST_ADDED` | `POST /bookings/{id}/addguest` | Validates booking ownership and expiry, saves guest details |
| `GUEST_ADDED` | `PAYMENT_PENDING` | `POST /bookings/{id}/payments` | Creates Stripe Checkout session, stores session ID on booking |
| `PAYMENT_PENDING` | `CONFIRMED` | Stripe webhook `checkout.session.completed` | Pessimistic-locks inventory, moves `reservedCount` to `bookedCount` (idempotent) |
| Any pre-confirmed | `CANCELLED` | `POST /bookings/{id}/cancel` | Releases reserved inventory |
| `CONFIRMED` | `CANCELLED` | `POST /bookings/{id}/cancel` | Releases booked inventory, issues Stripe refund |
| `RESERVED` / `GUEST_ADDED` / `PAYMENT_PENDING` | `CANCELLED` | Scheduled cleanup (TTL expired) | Releases reserved inventory automatically |

### Reservation TTL

Bookings that remain in `RESERVED`, `GUEST_ADDED`, or `PAYMENT_PENDING` state beyond the configurable TTL (default: 10 minutes) are automatically cancelled by `BookingCleanupService`, which runs every 60 seconds. Each expired booking is processed independently with per-booking error isolation to prevent one failure from blocking cleanup of other bookings.

---

## Dynamic Pricing Engine

### Decorator Chain

Pricing is calculated by composing strategies in a decorator chain:

```
HolidayPricingStrategy
  -> UrgencyPricingStrategy
    -> OccupancyPricingStrategy
      -> SurgePricingStrategy
        -> BasePricingStrategy
```

Each strategy wraps the previous one and applies its multiplier to the price returned by the inner strategy.

### Pricing Strategies

| Strategy | Condition | Multiplier | Description |
|----------|-----------|-----------|-------------|
| **Base** | Always | 1.0x | Returns `room.basePrice` |
| **Surge** | Always | `inventory.surgeFactor` | Manager-configurable per-day multiplier (0.1 - 10.0) |
| **Occupancy** | `bookedCount / totalCount > 80%` | 1.2x | Increases price when a room type is nearly full |
| **Urgency** | Booking date within 7 days | 1.15x | Last-minute booking premium |
| **Holiday** | Dec 24, Dec 25, Dec 31, Jan 1 | 1.25x | Peak holiday surcharge |

### Price Calculation Example

For a room with base price 5000, surge factor 1.5, 85% occupancy, booking within 7 days, on Dec 25:

```
Base:      5000
Surge:     5000 * 1.5     = 7500
Occupancy: 7500 * 1.2     = 9000   (>80% occupied)
Urgency:   9000 * 1.15    = 10350  (within 7 days)
Holiday:   10350 * 1.25   = 12937.50 (Christmas)
```

### Scheduled Price Updates

`PricingUpdateService` runs every hour (`0 0 * * * *`) and:

1. Iterates through all hotels in batches of 100
2. Submits each hotel to `HotelPricingUpdateWorker` via async thread pool (`pricingExecutor`)
3. The worker recalculates dynamic prices for all inventory rows for the next year
4. Updates `HotelPrice` table with the daily minimum price across all room types per hotel

The async thread pool (`pricingExecutor`) has 4 core threads, max 8, with a queue capacity of 100.

---

## Inventory Management

### How Inventory Works

Each `Inventory` record represents availability for a specific room type on a specific date:

```
| Field         | Description                                      |
|---------------|--------------------------------------------------|
| totalCount    | Total rooms of this type in the hotel             |
| bookedCount   | Rooms confirmed (paid) for this date              |
| reservedCount | Rooms temporarily held (pending payment)          |
| Available     | totalCount - bookedCount - reservedCount           |
| surgeFactor   | Manager-set price multiplier for this date         |
| price         | Current dynamic price (recalculated hourly)       |
| closed        | If true, this date is not bookable                |
```

### Inventory Lifecycle

1. **Creation**: When a hotel is activated or a room is added to an active hotel, inventory records are bulk-created for the next 365 days (batched in groups of 100 using Hibernate batch inserts)
2. **Reservation**: `initBooking()` atomically increments `reservedCount` via a bulk UPDATE with availability guards
3. **Confirmation**: `confirmBooking()` atomically decrements `reservedCount` and increments `bookedCount`
4. **Cancellation (pre-payment)**: `releaseReservedInventory()` decrements `reservedCount`
5. **Cancellation (post-payment)**: `cancelBooking()` decrements `bookedCount`
6. **Deletion**: All inventory is deleted when a hotel is deactivated or a room is deleted

### Concurrency Control

The booking flow uses **pessimistic write locks** (`SELECT ... FOR UPDATE`) to prevent race conditions:

```sql
-- Step 1: Lock rows and verify availability
SELECT * FROM inventory
WHERE room_id = :roomId AND date BETWEEN :start AND :end
  AND closed = false
  AND (totalCount - bookedCount - reservedCount) >= :roomsCount
FOR UPDATE;

-- Step 2: Atomically reserve
UPDATE inventory
SET reservedCount = reservedCount + :roomsCount
WHERE room_id = :roomId AND date BETWEEN :start AND :end
  AND (totalCount - bookedCount - reservedCount) >= :roomsCount
  AND closed = false;
```

---

## Stripe Payment Integration

### Payment Flow

```
1. Client: POST /bookings/{id}/payments
   Server: Creates Stripe Customer + Checkout Session
           Stores session ID on booking
           Returns session URL

2. Client: Redirects to Stripe Checkout page

3. Stripe: User completes payment
           Sends webhook to POST /api/v1/webhooks/payment

4. Server: Validates webhook signature (Stripe-Signature header)
           Looks up booking by paymentSessionId
           Idempotency check (skip if already CONFIRMED or CANCELLED)
           Pessimistic-locks inventory
           Moves reservedCount -> bookedCount
           Sets booking status to CONFIRMED
```

### Refund Flow

When a confirmed booking is cancelled:

1. Retrieves the Stripe session by `paymentSessionId`
2. Extracts the `paymentIntent` from the session
3. Creates a full refund via `Refund.create()`

### Configuration

| Property | Description | Default |
|----------|------------|---------|
| `STRIPE_KEY` | Stripe secret API key | Required |
| `WEBHOOK_SECRET` | Stripe webhook signing secret | Required |
| `STRIPE_CURRENCY` | Payment currency | `inr` |
| `STRIPE_MIN_AMOUNT` | Minimum charge in smallest currency unit (paise) | `5000` |

---

## API Reference

Base URL: `/api/v1`

### Authentication

| Method | Endpoint | Auth | Rate Limited | Description |
|--------|----------|------|-------------|-------------|
| POST | `/auth/register` | No | No | Register a new user (GUEST role) |
| POST | `/auth/login` | No | 5/min | Authenticate and receive tokens |
| POST | `/auth/logout` | No | No | Clear refresh token cookie |
| POST | `/auth/refresh-token` | Cookie | No | Get new access token using refresh token |

### Hotel Search (Public)

| Method | Endpoint | Auth | Rate Limited | Description |
|--------|----------|------|-------------|-------------|
| GET | `/hotels/search` | No | 30/min | Search hotels by city, dates, room count |
| GET | `/hotels/{hotelId}/info` | No | No | Get hotel details with room list |

**Search Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `city` | String | Yes | City name |
| `startDate` | LocalDate | Yes | Check-in date (today or future) |
| `endDate` | LocalDate | Yes | Check-out date (after startDate) |
| `roomsCount` | Integer | Yes | Number of rooms needed (min 1) |
| `page` | Integer | No | Page number (default 0) |
| `size` | Integer | No | Page size (default 10, max 100) |

### Bookings (Authenticated)

| Method | Endpoint | Auth | Rate Limited | Description |
|--------|----------|------|-------------|-------------|
| POST | `/bookings/init` | Yes | 10/min | Initialize booking (reserve inventory) |
| POST | `/bookings/{id}/addguest` | Yes | No | Add guest details |
| POST | `/bookings/{id}/payments` | Yes | No | Create Stripe payment session |
| POST | `/bookings/{id}/cancel` | Yes | No | Cancel booking (refund if paid) |
| GET | `/bookings/{id}/status` | Yes | No | Get booking status |
| GET | `/bookings/my-bookings` | Yes | No | List user's bookings (paginated) |

**Booking Request Body:**

```json
{
  "hotelId": 1,
  "roomId": 1,
  "checkInDate": "2026-05-01",
  "checkOutDate": "2026-05-03",
  "roomsCount": 1
}
```

**Guest Request Body (array):**

```json
[
  {
    "name": "John Doe",
    "gender": "MALE",
    "dateOfBirth": "1990-05-15"
  }
]
```

### User Profile (Authenticated)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/users/profile` | Yes | Get own profile |
| PUT | `/users/profile` | Yes | Update name, gender, date of birth |
| PUT | `/users/change-password` | Yes | Change password (requires current password) |

### Hotel Management (HOTEL_MANAGER)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/admin/hotel` | Manager | Create hotel (inactive by default) |
| GET | `/admin/hotel` | Manager | List own hotels (paginated) |
| GET | `/admin/hotel/{id}` | Manager | Get hotel details |
| PUT | `/admin/hotel/{id}` | Manager | Update hotel details |
| PATCH | `/admin/hotel/{id}/status?status=true` | Manager | Activate/deactivate hotel |
| DELETE | `/admin/hotel/{id}` | Manager | Delete hotel (cascades rooms, inventory) |
| GET | `/admin/hotel/{id}/bookings` | Manager | List hotel bookings (paginated) |
| GET | `/admin/hotel/{id}/report` | Manager | Revenue report (date range, defaults to last month) |

### Room Management (HOTEL_MANAGER)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/admin/hotels/{hotelId}/rooms` | Manager | Create room type |
| GET | `/admin/hotels/{hotelId}/rooms` | Manager | List all rooms in hotel |
| GET | `/admin/hotels/{hotelId}/rooms/{roomId}` | Manager | Get room details |
| PUT | `/admin/hotels/{hotelId}/rooms/{roomId}` | Manager | Update room details |
| DELETE | `/admin/hotels/{hotelId}/rooms/{roomId}` | Manager | Delete room (cascades inventory) |

### Inventory Management (HOTEL_MANAGER)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/admin/inventory/rooms/{roomId}` | Manager | Get all inventory rows for a room |
| PUT | `/admin/inventory/rooms/{roomId}` | Manager | Bulk update surge factor and/or closed status |

### Platform Admin (ADMIN)

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/admin/platform/users` | Admin | List all users (paginated) |
| GET | `/admin/platform/users/{id}` | Admin | Get user by ID |
| PUT | `/admin/platform/users/{id}/roles` | Admin | Update user roles |
| DELETE | `/admin/platform/users/{id}` | Admin | Delete user (checks for active bookings) |
| GET | `/admin/platform/hotels` | Admin | List all hotels (paginated) |
| PATCH | `/admin/platform/hotels/{id}/status?active=true` | Admin | Toggle any hotel's active status |
| GET | `/admin/platform/bookings` | Admin | List all bookings (paginated) |
| GET | `/admin/platform/stats` | Admin | Platform statistics dashboard |

### Webhooks

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/webhooks/payment` | Stripe signature | Stripe payment webhook receiver |

### Standard API Response Format

All error responses follow this structure:

```json
{
  "message": "Error description",
  "status": false,
  "path": "/api/v1/endpoint",
  "timestamp": "2026-04-14T10:30:00",
  "correlationId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

Validation errors include field-level details:

```json
{
  "message": "Validation failed",
  "status": false,
  "path": "/api/v1/auth/register",
  "timestamp": "2026-04-14T10:30:00",
  "correlationId": "...",
  "data": {
    "email": "Please provide a valid email address",
    "password": "Password must contain at least one uppercase letter..."
  }
}
```

---

## Rate Limiting

Powered by Resilience4j with per-endpoint configurations:

| Endpoint | Limit | Period | Fallback |
|----------|-------|--------|----------|
| `POST /auth/login` | 5 requests | 1 minute | `APIException: Too many login attempts` |
| `POST /bookings/init` | 10 requests | 1 minute | `APIException: Too many booking requests` |
| `GET /hotels/search` | 30 requests | 1 minute | `APIException: Too many search requests` |

All other endpoints use the default limit of 100 requests per minute.

---

## Caching Strategy

In-memory caching with Caffeine:

| Cache Name | Max Size | TTL | Eviction Strategy | What It Caches |
|------------|----------|-----|--------------------|----------------|
| `hotelSearch` | 500 entries | 5 minutes | Write-based | Search results by city + dates + rooms + pagination |
| `hotelInfo` | 1000 entries | 30 minutes | Write-based | Hotel details with room list |
| `platformStats` | 1 entry | 1 hour | Write-based | Admin dashboard aggregate stats |

Cache eviction is triggered on hotel create, update, delete, and status change operations.

---

## Observability

### Correlation IDs

Every request receives an `X-Correlation-ID` header. If the client sends one, it is propagated; otherwise a UUID is generated. The ID is placed in the SLF4J MDC for structured logging and included in all API error responses.

### Prometheus Metrics

Exposed at `/actuator/prometheus` (requires ADMIN role). Key business metrics:

| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `auth.login.success` | Counter | `user` | Successful login count per user |
| `auth.token.refresh` | Counter | - | Token refresh count |
| `bookings.initiated` | Counter | - | Bookings created |
| `bookings.cancelled.user` | Counter | - | User-initiated cancellations |
| `hotels.search.requests` | Counter | `city` | Search requests per city |
| `stripe.webhook.received` | Counter | `type` | Stripe webhooks processed by event type |

### Actuator Endpoints

| Endpoint | Access | Description |
|----------|--------|-------------|
| `/actuator/health` | Public | Application health check |
| `/actuator/info` | Admin | Application info |
| `/actuator/metrics` | Admin | Micrometer metrics |
| `/actuator/prometheus` | Admin | Prometheus scrape endpoint |

### Audit Logging

Admin actions are persisted asynchronously to the `audit_logs` table:

| Action | Trigger |
|--------|---------|
| `ROLE_UPDATE` | Admin changes a user's roles |
| `USER_DELETE` | Admin deletes a user |
| `HOTEL_STATUS_CHANGE` | Admin toggles hotel active status |

Each audit record captures: action, actor email, target entity/ID, details, client IP address, and timestamp.

---

## Error Handling

Centralized via `GlobalExceptionHandler` (`@RestControllerAdvice`):

| Exception | HTTP Status | Scenario |
|-----------|-------------|----------|
| `MethodArgumentNotValidException` | 400 Bad Request | Jakarta validation failures (field-level errors) |
| `APIException` | 400 Bad Request | Business logic violations |
| `BadCredentialsException` | 401 Unauthorized | Invalid email/password |
| `UnAuthorisedException` | 403 Forbidden | User lacks permission for the resource |
| `AccessDeniedException` | 403 Forbidden | Spring Security role check failure |
| `ResourceNotFoundException` | 404 Not Found | Entity not found by ID |
| `UserNotFoundException` | 404 Not Found | User not found |
| `RoomNotAvailableException` | 409 Conflict | Inventory unavailable for requested dates |
| `UserAlreadyExistsException` | 409 Conflict | Duplicate email on registration |
| `InvalidBookingStateException` | 409 Conflict | Invalid state transition |
| `BookingExpiredException` | 410 Gone | Booking TTL exceeded |
| `Exception` (catch-all) | 500 Internal Server Error | Unhandled errors (logged, generic message returned) |

Error messages never leak internal IDs or system details. The catch-all handler logs the full stack trace server-side.

---

## Security

### Input Validation

All request DTOs use Jakarta Bean Validation:

- **Registration**: Name (2-50 chars, letters/spaces only), Email (RFC-compliant), Password (8-72 chars, must include uppercase, lowercase, digit, and special character)
- **Booking**: Future/present dates, minimum 1 room
- **Hotel**: Non-blank name and city, valid contact (email, phone pattern)
- **Room**: Positive base price, non-negative total count, minimum capacity 1
- **Inventory**: Valid date range, surge factor 0.1-10.0

### Password Security

- BCrypt encoding with default strength
- Password change requires current password verification
- Maximum password length 72 characters (BCrypt limit)
- Write-only JSON serialization (password never returned in responses)

### Webhook Security

Stripe webhooks are verified using the `Stripe-Signature` header against the configured webhook secret. Invalid signatures return 400 Bad Request.

### CORS

Configurable via `CORS_ALLOWED_ORIGINS` environment variable. Defaults to `http://localhost:3000,http://localhost:5173`. Supports credentials (cookies).

---

## Getting Started

### Prerequisites

- Java 21+
- PostgreSQL 16+
- Gradle 9+
- Stripe account (Test Mode)

### 1. Clone and Configure

```bash
git clone <repository-url>
cd BookingSystem
```

Create a `.env` file in the project root:

```env
# Application
SPRING_APPLICATION_NAME=StayEase

# Database
DB_URL=jdbc:postgresql://localhost:5432/HotelDB
DB_USERNAME=postgres
DB_PASSWORD=your_password
JPA_DDL_AUTO=update

# JWT
JWT_SECRET=your-secret-key-at-least-32-bytes-long-for-hmac-sha256
JWT_EXPIRATION=3600000
JWT_REFRESH_TOKEN=604800000

# Stripe
STRIPE_KEY=sk_test_your_stripe_secret_key
WEBHOOK_SECRET=whsec_your_webhook_signing_secret

# Optional
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
FRONTEND_URL=http://localhost:8080
BOOKING_TTL_MINUTES=10
STRIPE_CURRENCY=inr
STRIPE_MIN_AMOUNT=5000
LOG_LEVEL=INFO
HIKARI_MAX_POOL_SIZE=10
HIKARI_MIN_IDLE=5
```

Generate a secure JWT secret:

```bash
openssl rand -base64 64
```

### 2. Start PostgreSQL

```bash
docker run --name hotel-postgres \
  -e POSTGRES_PASSWORD=your_password \
  -e POSTGRES_DB=HotelDB \
  -p 5432:5432 \
  -d postgres:16
```

### 3. Build and Run

```bash
# Build
./gradlew build

# Run
./gradlew bootRun
```

### 4. Access

- **API Base URL**: `http://localhost:8080/api/v1`
- **Swagger UI**: `http://localhost:8080/api/v1/swagger-ui.html`
- **Health Check**: `http://localhost:8080/api/v1/actuator/health`

### 5. Set Up Stripe Webhooks (for payments)

```bash
# Install Stripe CLI, then forward webhooks to local server
stripe listen --forward-to localhost:8080/api/v1/webhooks/payment
```

Use the webhook signing secret printed by the CLI as your `WEBHOOK_SECRET`.

---

## Configuration Reference

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `SPRING_APPLICATION_NAME` | Yes | - | Application name for metrics tags |
| `DB_URL` | Yes | - | PostgreSQL JDBC URL |
| `DB_USERNAME` | Yes | - | Database username |
| `DB_PASSWORD` | Yes | - | Database password |
| `JWT_SECRET` | Yes | - | HMAC-SHA signing key (min 32 bytes) |
| `JWT_EXPIRATION` | Yes | - | Access token lifetime in milliseconds |
| `JWT_REFRESH_TOKEN` | Yes | - | Refresh token lifetime in milliseconds |
| `STRIPE_KEY` | Yes | - | Stripe secret API key |
| `WEBHOOK_SECRET` | Yes | - | Stripe webhook signing secret |
| `JPA_DDL_AUTO` | No | `validate` | Hibernate DDL mode (`update` for dev) |
| `HIKARI_MAX_POOL_SIZE` | No | `10` | Max database connections |
| `HIKARI_MIN_IDLE` | No | `5` | Min idle connections |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:3000,http://localhost:5173` | Comma-separated allowed origins |
| `FRONTEND_URL` | No | `http://localhost:8080` | Frontend URL for Stripe redirect URLs |
| `BOOKING_TTL_MINUTES` | No | `10` | Reservation expiry time in minutes |
| `STRIPE_CURRENCY` | No | `inr` | Stripe payment currency |
| `STRIPE_MIN_AMOUNT` | No | `5000` | Minimum charge (smallest currency unit) |
| `LOG_LEVEL` | No | `INFO` | Application log level |

---

## Testing

```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "com.bookingsystem.service.impl.BookingServiceImplTest"
```

### Test Coverage

| Test Class | Coverage Area |
|------------|---------------|
| `AuthControllerTest` | Registration, login, token refresh |
| `WebhookControllerTest` | Stripe webhook signature validation, payment capture |
| `JwtServiceTest` | Token generation, validation, parsing |
| `AdminServiceImplTest` | User management, hotel oversight, platform stats |
| `BookingServiceImplTest` | Booking lifecycle, cancellation, payment flow |
| `BookingCleanupServiceTest` | TTL expiry, scheduled cleanup, error isolation |
| `HotelServiceImplTest` | Hotel CRUD, activation/deactivation |
| `InventoryServiceImplTest` | Inventory initialization, search, updates |
| `RoomServiceImplTest` | Room CRUD, ownership validation |
| `PricingServiceTest` | Decorator chain, individual strategy multipliers |
