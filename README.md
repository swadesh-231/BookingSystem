# Hotel & Room Management System

A robust, enterprise-grade Spring Boot application for managing hotel chains, rooms, and automated inventory systems. This module focuses on the administrative capabilities for onboarding hotels and managing their capacity logic.

## 🚀 Key Features

### 🏨 Hotel Management
*   **Onboarding**: Register new hotels with comprehensive contact and location details.
*   **Duplicate Prevention**: Smart validation to prevent duplicate hotel entries based on Name and City.
*   **Lifecycle Management**: Activate or deactivate hotels with a single API call.
*   **Cascade Operations**: Deleting a hotel automatically cleans up all associated rooms and inventory.

### 🛏️ Room Management
*   **Hierarchical Structure**: Rooms are strictly tightly coupled to their parent Hotel.
*   **Capacity Planning**: maintain total counts and guest capacity per room type.
*   **Validation**: Strict constraints on pricing (non-negative), counts, and types.

### 🔄 Automated Inventory System
*   **Zero-Touch Initialization**: 
    *   When a **Room** is created, if the hotel is active, inventory is automatically generated for the next 365 days.
    *   When a **Hotel** is activated, inventory is backfilled/initialized for *all* its rooms.
*   **Consistent Cleanup**: Cascade deletion ensures no orphaned inventory records remain when rooms or hotels are removed.

---

## 🛠 Tech Stack & Architecture

*   **Core**: Java 17, Spring Boot 3.x
*   **Database**: PostgreSQL with Spring Data JPA
*   **Architecture**: Layered (Controller -> Service -> Repository)
*   **DTO Pattern**: 
    *   `*Request` DTOs for input (Strict Validation)
    *   `*Response` DTOs for output (Clean Serialization)
*   **Mapping**: ModelMapper for entity-DTO conversion
*   **Utilities**: Lombok for boilerplate reduction

---

## 🔗 API Documentation

### 1. Create a New Hotel
**Endpoint**: `POST /admin/hotel`

**Request Body (`HotelRequest`)**:
```json
{
  "name": "Grand Plaza",
  "city": "New York",
  "contact": {
    "address": "123 Broadway St",
    "phoneNumber": "+1 212-555-0199",
    "email": "contact@grandplaza.com",
    "location": "40.7128° N, 74.0060° W"
  },
  "photos": ["url1.jpg", "url2.jpg"],
  "amenities": ["Spa", "Gym", "Pool"]
}
```

**Response (`HotelResponse`)**:
```json
{
  "id": 101,
  "name": "Grand Plaza",
  "city": "New York",
  "active": false,
  "contact": { ... },
  "photos": [...],
  "amenities": [...]
}
```

### 2. Activate Hotel (Triggers Inventory)
**Endpoint**: `PATCH /admin/hotel/{id}/status?status=true`

*   **Effect**: Sets `active=true` and generates inventory for all rooms for 1 year.

### 3. Add Room to Hotel
**Endpoint**: `POST /admin/hotels/{hotelId}/rooms`

**Request Body (`RoomRequest`)**:
```json
{
  "type": "Ocean View Suite",
  "basePrice": 250.00,
  "totalCount": 10,
  "capacity": 2,
  "photos": ["suite1.jpg"],
  "amenities": ["King Bed", "Mini Bar"]
}
```

**Response (`RoomResponse`)**:
```json
{
  "id": 505,
  "type": "Ocean View Suite",
  "basePrice": 250.00,
  "totalCount": 10,
  "capacity": 2,
  ...
}
```

### 4. Update Hotel Details
**Endpoint**: `PUT /admin/hotel/{id}`

*   **Logic**: Updates mutable fields. If Name or City changes, re-checks for duplicates. Preserves 'Active' status.

---

## ⚙️ Business Logic Details

### Duplicate Checking
The system enforces uniqueness on the pair `(Name, City)`.
*   *Attempting to create "Grand Hotel" in "Paris" when one already exists will throw an `APIException`.*

### Inventory Logic (`InventoryService`)
The system treats Inventory as a downstream effect of Room availability.
*   **Creation**: `inventoryService.initializeRoomForAYear(room)` is called automatically during `createRoom` (if hotel active) or `updateHotelStatus` (if activating).
*   **Deletion**: `inventoryService.deleteAllInventories(room)` is called before deleting any Room entity to maintain referential integrity.

---

## 🚀 Getting Started

1.  **Prerequisites**:
    *   Java 17+
    *   PostgreSQL running locally
    *   Gradle

2.  **Configuration**:
    Update `src/main/resources/application.properties`:
    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5432/booking_db
    spring.datasource.username=postgres
    spring.datasource.password=yourpassword
    ```

3.  **Run Application**:
    ```bash
    ./gradlew bootRun
    ```

