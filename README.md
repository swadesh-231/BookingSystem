# 🏨 Hotel Booking System

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-F2F4F9?style=for-the-badge&logo=spring-boot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Stripe](https://img.shields.io/badge/Stripe-626CD9?style=for-the-badge&logo=Stripe&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=JSON%20web%20tokens)

A comprehensive, enterprise-grade Spring Boot application for hotel management, booking orchestration, and dynamic pricing. This system facilitates secure, real-time reservations with integrated Stripe payments and robust inventory control.

---

## 📋 Table of Contents

- [Key Features](#-key-features)
- [System Architecture](#-system-architecture)
- [Tech Stack](#-tech-stack)
- [Database Design](#-database-design)
- [API Documentation](#-api-documentation)
- [Security Implementation](#-security-implementation)
- [Getting Started](#-getting-started)
- [Configuration](#-configuration)

---

## ✨ Key Features

### 🏨 Core Hotel Operations
- **Hotel Management**: Onboard hotels, manage amenities, and control lifecycle status.
- **Room Inventory**: Hierarchical room management with capacity planning and real-time availability tracking.
- **Smart Search**: Filter hotels by city, dates, and guest capacity with pagination support.

### 💰 Dynamic Pricing Engine
Sophisticated strategy pattern implementation for calculating room rates:
- **Base Pricing**: Standard room rates.
- **Surge Pricing**: Automatically adjusts rates based on demand factors.
- **Occupancy Pricing**: Incremental pricing based on current booking levels (>80%).
- **Urgency Pricing**: Premium rates for last-minute bookings (within 7 days).
- **Holiday Pricing**: configurable holiday markups.

### 📅 Booking & Payments
- **Secure Reservations**: End-to-end booking lifecycle management.
- **Stripe Integration**: PCI-DSS compliant checkout sessions for payments.
- **Transaction Safety**: Minimum transaction amount enforcement (₹50) to prevent gateway errors.

---

## 🏗 System Architecture

The application follows a clean, layered monolithic architecture designed for maintainability and scalability.

```
┌─────────────────────────────────────────────────────────────┐
│                   Presentation Layer                        │
│   REST Controllers (Auth, Hotel, Booking, Payment API)      │
└─────────────────────────────┬───────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────┐
│                      Service Layer                          │
│    Business Logic • Pricing Strategies • Payment Processing │
└─────────────────────────────┬───────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────┐
│                    Data Access Layer                        │
│           Spring Data JPA Repositories & Entities           │
└─────────────────────────────┬───────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────┐
│                       PostgreSQL                            │
│                  Relational Persistence                     │
└─────────────────────────────────────────────────────────────┘
```

### Design Patterns
- **Strategy Pattern**: Decouples pricing logic (`PricingStrategy`) from the core booking service, allowing flexible rate calculations.
- **DTO Pattern**: Using `ModelMapper` to separate internal entities from external API contracts.
- **Repository Pattern**: Abstraction layer for data access.

---

## 🛠 Tech Stack

| Component | Technology | Description |
|-----------|------------|-------------|
| **Core** | Java 21 | Latest LTS release |
| **Framework** | Spring Boot 3.x | Web, Data JPA, Security, Validation |
| **Database** | PostgreSQL | Relational data store |
| **Security** | Spring Security + JWT | Stateless authentication |
| **Payments** | Stripe API | Payment processing gateway |
| **Build Tool** | Gradle | Dependency management |
| **Utilities** | Lombok, ModelMapper | Boilerplate reduction & mapping |

---

## 🗄 Database Design

The database schema is normalized to ensure data integrity and efficient querying.

### Entity Relationship Diagram

![ER Diagram](Booking.png)

---

## 📡 API Documentation

### Authentication
- **Register**: `POST /auth/register`
- **Login**: `POST /auth/login` (Returns JWT)

### Hotel Management (Admin)
- **Create Hotel**: `POST /admin/hotel`
- **Add Room**: `POST /admin/hotels/{id}/rooms`
- **Toggle Status**: `PATCH /admin/hotel/{id}/status`

### Public Endpoints
- **Search Hotels**: `GET /hotels/search?city=NY&startDate=...`
- **Hotel Info**: `GET /hotels/{id}/info`

### Booking Operations
- **Initialize**: `POST /bookings/init`
- **Add Guests**: `POST /bookings/{id}/addguest`
- **Pay**: `POST /bookings/{id}/payments`
- **Cancel**: `POST /bookings/{id}/cancel`

---

## 🔐 Security Implementation

- **Stateless Authentication**: Utilizes `JwtAuthFilter` to validate tokens on every request.
- **RBAC (Role-Based Access Control)**:
  - `HOTEL_MANAGER`: Full access to administrative endpoints (`/admin/**`).
  - `Authenticated User`: Access to personal booking endpoints (`/bookings/**`).
  - `Public`: Open access to search and auth endpoints.
- **Password Security**: BCrypt hashing for all user passwords.

---

## 🚀 Getting Started

### Prerequisites
- JDK 21
- PostgreSQL
- Gradle 8+

### Database Setup
Ensure PostgreSQL is running and create a database:
```sql
CREATE DATABASE booking_db;
```

### Configuration
Create a `.env` file in the root directory:

```properties
DB_URL=jdbc:postgresql://localhost:5432/booking_db
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_SECRET=your_jwt_secret_key_must_be_long_enough
STRIPE_KEY=sk_test_...
```

### Build & Run
```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun
```
The application will start at `http://localhost:8080`.

---

## 🤝 Contributing

Contributions are welcome! Please fork the repository and submit a pull request for review.

1. Fork the repo
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
