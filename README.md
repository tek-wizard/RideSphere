#  Ride-Sharing Backend (Uber Clone)

## Project Overview
This is a backend implementation for a mini Ride-Sharing application built as part of my coursework. It handles the core logic for a booking system where Passengers can request rides and Drivers can accept them. 

The project focuses on **Backend Logic, Data Handling, and Security** using Spring Boot and MongoDB.

---

##  Implemented Features

### Core Functionality
* **User Roles:** Separate logic for `PASSENGER` and `DRIVER`.
* **Security:** JWT (JSON Web Token) authentication. Users get a token upon login which acts as their digital ID.
* **Ride Lifecycle:** Full flow from *Ride Requested* → *Accepted* → *Completed*.

### Advanced Features (Analytics & Search)
* **Analytics Dashboard:** APIs to track Driver Earnings, User Spending, and Daily Ride Counts using **MongoDB Aggregations**.
* **Smart Search:** Regex-based search to find rides by location name (e.g., "Airport").
* **Filtering:** Filters for Ride Distance (e.g., 5km-10km), Date Ranges, and Status.
* **Sorting:** Ability to sort available rides by Fare.

---

##  Technical Architecture
I followed a **Clean Architecture** approach to keep the code organized and readable:

1.  **Controller:** The entry point. Handles the HTTP requests and input validation.
2.  **Service:** Contains the business rules (e.g., calculating fares, verifying driver status).
3.  **Repository:** Manages direct communication with the MongoDB database.
4.  **Global Exception Handler:** Catches errors centrally to return clean JSON error messages instead of server crashes.

---

##  Development Notes
**Solving the Circular Dependency**
One challenge I faced was a "Circular Dependency" error where `SecurityConfig` and `JwtFilter` were depending on each other, preventing the app from starting. 

I resolved this by refactoring the user authentication beans (`UserDetailsService`, `PasswordEncoder`) into a separate `ApplicationConfig` class. This broke the cycle and improved the code structure.

---

##  API Endpoints Summary

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| **POST** | `/api/auth/register` | Register a new user (User/Driver) |
| **POST** | `/api/auth/login` | Login and receive JWT Token |
| **POST** | `/api/v1/rides` | Create a new ride request |
| **GET** | `/api/v1/driver/rides/requests` | View pending rides (Driver only) |
| **POST** | `/api/v1/driver/rides/{id}/accept` | Accept a ride |
| **POST** | `/api/v1/rides/{id}/complete` | Mark ride as completed |
| **GET** | `/api/v1/search?text=...` | Search rides by location |
| **GET** | `/api/v1/analytics/driver/{id}/summary` | View driver earnings & stats |
