# Movie Booking System

A comprehensive REST API for managing movie theater bookings, built with Spring Boot. This system allows users to manage movies, theaters, shows, and bookings with robust validation and error handling.

## Features

### Core Functionality
- **Movie Management**: Add, update, delete, and search movies by title, genre, or language
- **Theater Management**: Create and manage theaters with different screen types and capacities
- **Show Management**: Schedule shows with specific movies, theaters, and timings
- **Booking System**: Complete booking infrastructure (in development)

### Key Highlights
- RESTful API architecture
- Comprehensive input validation
- Global exception handling
- ModelMapper for DTO transformations
- PostgreSQL database integration
- Support for multiple genres per movie
- Various screen types (2D, 3D, IMAX, 4DX, VIP)

## Technology Stack

- **Framework**: Spring Boot
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA (Hibernate)
- **Validation**: Jakarta Bean Validation
- **Mapping**: ModelMapper
- **Build Tool**: Maven/Gradle
- **Java Version**: 17+

## Database Schema

### Entities
- **Movie**: Stores movie information with genres, duration, language, and release date
- **Theater**: Contains theater details including location, capacity, and screen type
- **Show**: Links movies and theaters with specific show times and pricing
- **Booking**: Manages customer bookings (in development)
- **User**: User management (in development)

### Relationships
- Movie ↔ Show (One-to-Many)
- Theater ↔ Show (One-to-Many)
- Show ↔ Booking (One-to-Many)

## API Endpoints

### Movies
```
POST   /api/v1/movies/add-movie          - Add a new movie
GET    /api/v1/movies                    - Get all movies
GET    /api/v1/movies/{id}               - Get movie by ID
PUT    /api/v1/movies/{id}               - Update movie
DELETE /api/v1/movies/{id}               - Delete movie
GET    /api/v1/movies/genre?genre=ACTION - Search by genre
GET    /api/v1/movies/language?language= - Search by language
GET    /api/v1/movies/title?title=       - Search by exact title
```

### Theaters
```
POST   /api/v1/theater/add-theater       - Create a new theater
GET    /api/v1/theater?location=         - Get theaters by location
PUT    /api/v1/theater/{id}              - Update theater
DELETE /api/v1/theater/{id}              - Delete theater
```

### Shows
```
POST   /api/v1/show/add-show             - Create a new show
GET    /api/v1/show                      - Get all shows
GET    /api/v1/show/by-theater?theater=  - Get shows by theater name
GET    /api/v1/show/by-movie?movie=      - Get shows by movie name
PUT    /api/v1/show/{showId}             - Update show
DELETE /api/v1/show/{showId}             - Delete show
```

## Request/Response Examples

### Add Movie
**Request:**
```json
POST /api/v1/movies/add-movie
{
  "name": "Inception",
  "description": "A mind-bending thriller about dreams within dreams",
  "duration": 148,
  "language": "English",
  "releaseDate": "2010-07-16",
  "genres": ["ACTION", "THRILLER"]
}
```

**Response:**
```json
{
  "id": 1,
  "name": "Inception",
  "description": "A mind-bending thriller about dreams within dreams",
  "duration": 148,
  "language": "English",
  "releaseDate": "2010-07-16",
  "genres": ["ACTION", "THRILLER"]
}
```

### Create Show
**Request:**
```json
POST /api/v1/show/add-show
{
  "showTime": "2024-12-29T18:30:00",
  "price": 250.00,
  "movieId": 1,
  "theaterId": 1
}
```

**Response:**
```json
{
  "showId": 1,
  "showTime": "2024-12-29T18:30:00",
  "price": 250.00,
  "movieId": 1,
  "movieName": "Inception",
  "theaterId": 1,
  "theaterName": "PVR Cinemas",
  "location": "Mumbai"
}
```

## Validation Rules

### Movie
- Name: 2-100 characters, required
- Description: Max 500 characters, required
- Duration: 30-300 minutes
- Release date: Cannot be in the future
- At least one genre required

### Theater
- Name: 2-100 characters, required
- Capacity: Minimum 10 seats
- Screen type: Required (2D, 3D, IMAX, etc.)

### Show
- Show time: Cannot be in the past
- Price: Must be positive
- Movie and Theater IDs: Required

## Error Handling

The system includes comprehensive error handling for:
- Validation errors (400 Bad Request)
- Resource not found (404 Not Found)
- Duplicate resources (409 Conflict)
- Business logic violations (400 Bad Request)
- Internal server errors (500)

**Error Response Format:**
```json
{
  "success": false,
  "message": "Error description",
  "data": null,
  "timestamp": "2024-12-28T10:30:00"
}
```

## Setup Instructions

### Prerequisites
- Java 17 or higher
- PostgreSQL database
- Maven or Gradle

### Database Configuration
1. Create a PostgreSQL database:
```sql
CREATE DATABASE Booking;
```

2. Update `application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/Booking
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

### Running the Application
```bash
# Using Maven
mvn spring-boot:run

# Using Gradle
gradle bootRun
```

The application will start on `http://localhost:8080`

## Enums

### Genre
`ACTION`, `COMEDY`, `DRAMA`, `THRILLER`

### Screen Type
`TWO_D`, `THREE_D`, `IMAX`, `IMAX_3D`, `FOUR_DX`, `VIP`

### Booking Status
`CONFIRMED`, `CANCELED`, `PENDING`

## Project Structure
```
src/main/java/com/bookingsystem/
├── config/          - Configuration classes
├── controller/      - REST controllers
├── dto/             - Data Transfer Objects
├── entity/          - JPA entities
├── exception/       - Custom exceptions and handlers
├── repository/      - Spring Data repositories
├── security/        - Security configuration (in development)
└── service/         - Business logic layer
```

## Future Enhancements
- [ ] User authentication and authorization
- [ ] Complete booking flow
- [ ] Payment integration
- [ ] Seat selection mechanism
- [ ] Email notifications
- [ ] Admin dashboard
- [ ] Search with filters
- [ ] Rating and review system

