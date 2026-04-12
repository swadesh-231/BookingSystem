# BookingSystem

Hotel booking system built with Spring Boot 4.0.2 and Java 21.

## Build & Run

- **Build**: `./gradlew build`
- **Compile only**: `./gradlew compileJava`
- **Run tests**: `./gradlew test`
- **Run app**: `./gradlew bootRun` (requires env vars, see below)

## Environment

Requires a `.env` file or environment variables:

**Required:**
- `SPRING_APPLICATION_NAME`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` (PostgreSQL)
- `JWT_SECRET` (min 32 bytes), `JWT_EXPIRATION`, `JWT_REFRESH_TOKEN`
- `STRIPE_KEY`, `WEBHOOK_SECRET`

**Optional (with defaults):**
- `JPA_DDL_AUTO` (default: `validate`) - use `update` for dev
- `HIKARI_MAX_POOL_SIZE` (default: `10`), `HIKARI_MIN_IDLE` (default: `5`)
- `CORS_ALLOWED_ORIGINS` (default: `http://localhost:3000,http://localhost:5173`)
- `FRONTEND_URL` (default: `http://localhost:8080`)
- `BOOKING_TTL_MINUTES` (default: `10`) - reservation expiry time
- `STRIPE_CURRENCY` (default: `inr`), `STRIPE_MIN_AMOUNT` (default: `5000`)
- `LOG_LEVEL` (default: `INFO`) - app logging level

## Tech Stack

- Spring Boot 4.0.2 (webmvc, JPA, Security, Actuator, Validation)
- PostgreSQL, Hibernate, HikariCP connection pool
- JWT auth (jjwt 0.13.0), BCrypt password encoding
- Stripe payments integration
- Lombok, ModelMapper, SpringDoc OpenAPI (Swagger)
- Gradle build system

## Project Structure

```
src/main/java/com/bookingsystem/
  config/          - ProjectConfig (ModelMapper), PasswordEncoderConfig, StripeConfig, WebConfig, OpenApiConfig
  controller/      - REST controllers: Admin, Auth, Booking, Hotel, HotelSearch, Inventory, Room, User, Webhook
  dto/             - Request/response DTOs (with Jakarta validation)
  entity/          - JPA entities: User, Hotel, Room, Booking, Inventory, Guest, HotelContact, HotelPrice
  entity/enums/    - Role, Gender, BookingStatus, PaymentStatus
  exception/       - Global exception handler, custom exceptions, AuthEntryPointJwt
  repository/      - Spring Data JPA repositories
  security/        - SecurityConfig, jwt/ (JwtAuthFilter, JwtService), utils/AuthUtils
  service/         - Service interfaces and impl/ implementations (incl. AdminService)
  strategy/        - Pricing strategies (Base, Surge, Occupancy, Urgency, Holiday), PricingService, PricingUpdateService
```

## Key Architecture Decisions

- **PasswordEncoder is in its own config** (`config/PasswordEncoderConfig`) to avoid circular dependency with SecurityConfig/JwtAuthFilter/UserServiceImpl
- Security is stateless (JWT, no sessions). Context path: `/api/v1`
- Security headers: X-Frame-Options DENY, X-Content-Type-Options, HSTS enabled
- Actuator: `/actuator/health` is public, other endpoints require ADMIN role
- User entity implements `UserDetails`; UserServiceImpl implements both `UserService` and `UserDetailsService`
- Pricing uses Strategy pattern (decorator chain) with multiple implementations
- Roles: `GUEST`, `HOTEL_MANAGER`, `ADMIN` (mapped as `ROLE_` prefixed authorities)
- Admin endpoints under `/admin/platform/**` (ADMIN role), hotel management under `/admin/**` (HOTEL_MANAGER role)
- Admin actions are audit-logged with `ADMIN_AUDIT:` prefix
- All admin list endpoints are paginated (Page<T>)
- Booking cleanup runs every 60s (fixedDelay) with per-booking error isolation
- CORS origins configurable via `CORS_ALLOWED_ORIGINS` env var

## Conventions

- Lombok `@RequiredArgsConstructor` for constructor injection
- DTOs for all request/response payloads with Jakarta validation annotations
- Service layer interfaces with `impl/` package for implementations
- `AuthUtils.getCurrentUser()` for getting the authenticated user
- Error messages do not leak internal IDs or system details
