package com.prateek.uber.controller;

import com.prateek.uber.dto.RideRequest;
import com.prateek.uber.model.Ride;
import com.prateek.uber.service.DriverLocationService;
import com.prateek.uber.service.RideSearchService;
import com.prateek.uber.service.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;
    private final RideSearchService searchService;
    private final DriverLocationService locationService;

    // Core Ride Lifecycle Endpoints

    @PostMapping("/rides")
    public ResponseEntity<Ride> createRide(@RequestBody @Valid RideRequest request) {
        return ResponseEntity.ok(rideService.createRide(request));
    }

    @GetMapping("/user/rides")
    public ResponseEntity<List<Ride>> getUserRides() {
        return ResponseEntity.ok(rideService.getUserRides());
    }

    @PostMapping("/rides/{rideId}/complete")
    public ResponseEntity<Ride> completeRide(@PathVariable String rideId) {
        return ResponseEntity.ok(rideService.completeRide(rideId));
    }

    // Driver Operations (Geospatial & Dispatch)

    @GetMapping("/driver/rides/requests")
    public ResponseEntity<List<Ride>> getPendingRides() {
        return ResponseEntity.ok(rideService.getPendingRides());
    }

    @PostMapping("/driver/rides/{rideId}/accept")
    public ResponseEntity<Ride> acceptRide(@PathVariable String rideId) {
        return ResponseEntity.ok(rideService.acceptRide(rideId));
    }

    @GetMapping("/driver/{driverId}/active-rides")
    public ResponseEntity<List<Ride>> getDriverActiveRides(@PathVariable String driverId) {
        return ResponseEntity.ok(searchService.getRidesByFieldAndStatus("driverId", driverId, "ACCEPTED"));
    }

    // Redis: Updates driver's real-time location bucket
    @PostMapping("/driver/location")
    public ResponseEntity<String> updateLocation(
            @RequestParam String driverId,
            @RequestParam double lat,
            @RequestParam double lon) {
        locationService.updateDriverLocation(driverId, lat, lon);
        return ResponseEntity.ok("Location Updated");
    }

    // Redis: O(1) Lookup for drivers in the current grid
    @GetMapping("/nearby-drivers")
    public ResponseEntity<Set<String>> getNearbyDrivers(
            @RequestParam double lat,
            @RequestParam double lon) {
        return ResponseEntity.ok(locationService.getNearbyDrivers(lat, lon));
    }

    // Search & Analytics Endpoints

    @GetMapping("/search")
    public ResponseEntity<List<Ride>> search(@RequestParam String text) {
        return ResponseEntity.ok(searchService.searchRides(text));
    }

    @GetMapping("/advanced-search")
    public ResponseEntity<List<Ride>> advancedSearch(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String text,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(searchService.advancedSearch(status, text, page, size));
    }

    @GetMapping("/filter-status")
    public ResponseEntity<List<Ride>> filterByStatus(
            @RequestParam String status,
            @RequestParam String text) {
        return ResponseEntity.ok(searchService.advancedSearch(status, text, 0, 100));
    }

    @GetMapping("/filter-distance")
    public ResponseEntity<List<Ride>> filterDistance(@RequestParam Double min, @RequestParam Double max) {
        return ResponseEntity.ok(searchService.filterByDistance(min, max));
    }

    @GetMapping("/filter-date-range")
    public ResponseEntity<List<Ride>> filterDate(
            @RequestParam LocalDate start,
            @RequestParam LocalDate end) {
        return ResponseEntity.ok(searchService.filterByDate(start, end));
    }

    @GetMapping("/date/{date}")
    public ResponseEntity<List<Ride>> getRidesOnDate(@PathVariable LocalDate date) {
        return ResponseEntity.ok(searchService.getRidesByDate(date));
    }

    @GetMapping("/sort")
    public ResponseEntity<List<Ride>> sortByFare(@RequestParam String order) {
        return ResponseEntity.ok(searchService.sortByFare(order));
    }

    // Admin / User History Lookups
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Ride>> getRidesByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(searchService.getRidesByFieldAndStatus("userId", userId, null));
    }

    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<List<Ride>> getUserRidesByStatus(
            @PathVariable String userId, @PathVariable String status) {
        return ResponseEntity.ok(searchService.getRidesByFieldAndStatus("userId", userId, status));
    }
}