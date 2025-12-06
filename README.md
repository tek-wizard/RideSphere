#  Mini Ride-Sharing Backend (Uber Clone)

## 1. Project Overview (The "Big Picture")
This project is the **backend "brain"** for a mini Ride-Sharing application. 

Imagine a mobile app where a user clicks "Book Ride." The app talks to this code. This backend creates the logic that checks if the user is allowed to book, saves the ride details in the database, and lets a driver see the request. 

*Note: This project focuses strictly on the logic and data handling (Backend), not the visual interface (Frontend).*

---

## 2. Implemented Features
We created a set of **API Endpoints** (URLs) to handle the core functionality of the app.

###  Security (The Bouncer)
* **Registration:** Users can sign up as either a `PASSENGER` or a `DRIVER`. Passwords are encrypted (BCrypt) before saving, ensuring security.
* **Login (JWT):** Upon login, users receive a digital "badge" (JSON Web Token). This token must be shown with every subsequent request to prove identity and authorization.

###  Ride Logic (The Matchmaker)
* **Create Ride:** Passengers can initiate a trip request (e.g., "I want to go from A to B").
* **View Requests:** Drivers can view a list of all passengers currently waiting for a ride.
* **Accept Ride:** A driver can select a specific ride to accept, assigning themselves to that trip.
* **Complete Ride:** Once the trip is finished, the ride status is updated to "Completed."

---

## 3. Technical Architecture
The project follows the **Clean Architecture** pattern to ensure organization and scalability:

* ** Controller (The Receptionist):** The first point of contact. It receives incoming requests (e.g., "Book a ride") and performs input validation to ensure data integrity.
    
* ** Service (The Manager):** Contains the core business logic. It validates rules (e.g., "Is this user actually a driver?" or "Is this ride already taken?") before processing.
    
* ** Repository (The Librarian):** Handles all interactions with the MongoDB database to save, fetch, or update data.
    
* ** Global Exception Handler (The Safety Net):** A centralized error handling mechanism. If the system crashes or a user attempts unauthorized actions, this catches the error and returns a polite JSON response instead of a raw stack trace.

---

## 4. Development Notes: Circular Dependency Fix
During development, we encountered and resolved a **Circular Dependency** issue within the Spring Boot configuration.

### The Problem ("Chicken and Egg")
The `SecurityConfig` bean required the `JwtFilter`, but the `JwtFilter` required dependencies defined inside `SecurityConfig`. This created a loop where neither could start first.

### The Fix
We refactored the architecture by extracting the user-loading logic into a separate configuration file:
* **Moved:** `UserDetailsService`, `PasswordEncoder`, and `AuthenticationProvider`
* **To:** A new `ApplicationConfig` class.

This allowed both the Security Configuration and the Filter to access user data independently without locking each other.
