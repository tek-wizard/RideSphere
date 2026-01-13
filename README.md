# RideSphere

**RideSphere** is a high-performance backend engine designed to simulate the distributed architectural challenges of top-tier ride-hailing platforms. Unlike standard CRUD applications, this project prioritizes **system design reliability** over simple data storage, specifically engineering solutions for **concurrency race conditions, geospatial latency, and high-throughput scalability**.

It differentiates itself by strictly implementing industry-standard optimizations: transitioning geospatial searches from $O(N)$ to **$O(1)$ constant time via Redis Grid Sharding**, enforcing strong consistency with **Redlock distributed locking** to prevent double-bookings, and replacing resource-heavy polling with **event-driven WebSockets**. This infrastructure ensures sub-millisecond driver matching and API stability even under simulated high-load scenarios.

---

## Core Features & Functionality

### 1. Authentication & Security
* **Stateless Architecture:** Implements a fully stateless authentication mechanism using **JSON Web Tokens (JWT)**.
* **Role-Based Access Control (RBAC):** Granular security policies enforced via Spring Security.
    * `ROLE_USER`: Access to ride booking, history, and spending analytics.
    * `ROLE_DRIVER`: Access to ride requests, performance stats, and location updates.
* **Secure Filtering:** Custom `OncePerRequestFilter` chains (`JwtFilter`) ensure token validation prior to request processing.

### 2. Ride Lifecycle Management
* **State Machine Logic:** Manages the complex state transitions of a ride (`REQUESTED` $\rightarrow$ `ACCEPTED` $\rightarrow$ `COMPLETED`).
* **Transactional Integrity:** logic prevents invalid state jumps (e.g., a driver cannot complete a ride that has not been accepted).
* **Dynamic Data Models:** Utilizes **MongoDB** documents to store flexible ride data, including fare calculation, distance metrics, and timestamps.

### 3. Advanced Search Engine
* **Dynamic Query Building:** Bypasses standard repository methods in favor of `MongoTemplate` to construct complex, multi-criteria queries dynamically.
* **capabilities:**
    * **Text Search:** Case-insensitive regex matching for pickup/drop locations.
    * **Filters:** Multi-layered filtering by status, date ranges, and distance thresholds.
    * **Pagination:** Optimized `PageRequest` implementation for handling large datasets efficiently.

### 4. Data Analytics & Aggregation
* **Aggregation Pipelines:** Leverages MongoDB's powerful aggregation framework to perform server-side calculations, reducing application memory overhead.
* **Driver Performance:** Real-time calculation of total earnings, ride counts, and average distances per driver.
* **Financial Insights:** Aggregated spending history for passengers.
* **Temporal Analysis:** Time-series grouping to visualize ride volume trends over time (e.g., Rides Per Day).

---

## Distributed Systems Implementations

### 1. $O(1)$ Geo-Spatial Sharding
* **Problem:** Traditional geospatial queries (e.g., `$near`) degrade to $O(N)$ or $O(\log N)$ as the dataset grows.
* **Solution:** Implemented **Grid-Based Spatial Indexing** using **Redis**. The world is partitioned into discrete $1km \times 1km$ buckets.
* **Complexity:** Driver lookups are reduced to constant time **$O(1)$**, independent of the total driver count.

### 2. Distributed Locking (Redlock)
* **Problem:** Concurrent "Accept Ride" requests can lead to race conditions where multiple drivers are assigned the same ride.
* **Solution:** Integrated **Redisson** to implement the **Redlock** algorithm. Critical sections are guarded by distributed locks, ensuring strong consistency across multiple server instances.

### 3. Real-Time Event Streaming
* **Problem:** Polling for ride updates wastes bandwidth and increases server load.
* **Solution:** Implemented a full-duplex **WebSocket** communication layer using the **STOMP** protocol. Ride events are pushed asynchronously to drivers in real-time.

### 4. API Rate Limiting
* **Problem:** Vulnerability to DDoS attacks and API abuse.
* **Solution:** Deployed a **Token Bucket** algorithm via Redis at the filter chain level. This protects the core logic by rejecting excess traffic (`HTTP 429`) before it reaches database connections.

---

## Technology Stack

### Backend Infrastructure
* **Language:** Java 22
* **Framework:** Spring Boot 3.4
* **Build Tool:** Maven

### Data Persistence
* **MongoDB:** Primary document store for Users, Rides, and historical data. Used for its schema flexibility and aggregation capabilities.
* **Redis:** In-memory data store. Used for high-speed geospatial indexing, distributed locks, and rate limiting counters.

### DevOps & Tools
* **Docker:** Containerization of Redis and application services.
* **Redisson:** Redis Java client for distributed objects.
* **Lombok:** Boilerplate reduction.

---

## API Reference

### Authentication
* `POST /api/auth/register` - Register a new User or Driver (Requires role).
* `POST /api/auth/login` - Authenticate credentials and receive a JWT Bearer token.

### Ride Management (Core)
* `POST /api/v1/rides` - Create a new ride request (Broadcasts via WebSocket).
* `GET /api/v1/user/rides` - Get all rides for the currently logged-in user.
* `POST /api/v1/rides/{rideId}/complete` - Mark a ride as completed (Requires Accepted status).

### Driver Operations
* `GET /api/v1/driver/rides/requests` - View all pending (REQUESTED) rides.
* `POST /api/v1/driver/rides/{id}/accept` - Accept a ride. **(Protected by Distributed Redlock)**.
* `GET /api/v1/driver/{driverId}/active-rides` - Get currently active rides for a specific driver.
* `POST /api/v1/driver/location` - Ping driver's real-time location to Redis Grid.
* `GET /api/v1/nearby-drivers` - Fetch available driver IDs in the current 1km Grid Bucket **(O(1) Search)**.

### Search & Filtering Engine
* `GET /api/v1/search` - Basic text search on Pickup/Drop locations (Case-insensitive Regex).
* `GET /api/v1/advanced-search` - Multi-criteria filter (Status, Text, Pagination).
* `GET /api/v1/filter-status` - Filter rides by specific status (e.g., REQUESTED, COMPLETED).
* `GET /api/v1/filter-distance` - Find rides within a specific distance range (min/max km).
* `GET /api/v1/filter-date-range` - Find rides created between two dates.
* `GET /api/v1/date/{date}` - Get all rides that occurred on a specific calendar date.
* `GET /api/v1/sort` - Sort rides by Fare amount (ASC/DESC).

### Analytics & Reporting
* `GET /api/v1/analytics/driver/{driverId}/summary` - Aggregated earnings, ride count, and average distance for a driver.
* `GET /api/v1/analytics/user/{userId}/spending` - Total spending history for a user.
* `GET /api/v1/analytics/status-summary` - Statistical breakdown of rides by current status.
* `GET /api/v1/analytics/rides-per-day` - Time-series data showing daily ride volume.

### User Specific
* `GET /api/v1/user/{userId}` - Admin lookup for all rides by a specific User ID.
* `GET /api/v1/user/{userId}/status/{status}` - Lookup specific user rides filtered by status.

---

## Setup & Installation

Follow these steps to get the distributed infrastructure running locally.

### 1. Prerequisites
Ensure you have the following installed:
* **Java JDK 22** (or JDK 17+)
* **Docker & Docker Compose** (Required for Redis)
* **Maven** (Build tool)

### 2. Infrastructure Setup (Redis)
We use Redis for Distributed Locking, Geo-Sharding, and Rate Limiting. Run it via Docker:

```bash
docker run -d --name uber-redis -p 6379:6379 redis
```

### 3. Application Configuration

Update your `src/main/resources/application.properties` file to point to your local database instances.

#### application.properties

```properties
# MongoDB Configuration
spring.data.mongodb.uri=mongodb://localhost:27017/uber_db

# Redis Configuration (Critical for Redisson & Locking)
spring.data.redis.host=localhost
spring.data.redis.port=6379

# JWT Security
jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
jwt.expiration=86400000
```

### 4. Build and Run

Compile the application and start the Spring Boot server.

#### Build the Project

```bash
# Build the project (skipping tests for speed)
mvn clean install -DskipTests
# Run the application
java -jar target/uber-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`.

WebSocket Endpoint: `ws://localhost:8080/ws`  
API Entry Point: `http://localhost:8080/api/v1`



---

## License

This project is licensed under the MIT License.