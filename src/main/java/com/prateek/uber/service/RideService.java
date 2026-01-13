package com.prateek.uber.service;

import com.prateek.uber.dto.RideRequest;
import com.prateek.uber.model.Ride;
import com.prateek.uber.model.User;
import com.prateek.uber.repository.RideRepository;
import com.prateek.uber.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RedissonClient redissonClient;

    private static final String RIDE_TOPIC = "/topic/ride-requests";

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Ride createRide(RideRequest request) {
        User user = getCurrentUser();

        Ride ride = new Ride();
        ride.setUserId(user.getId());
        ride.setPickupLocation(request.getPickupLocation());
        ride.setDropLocation(request.getDropLocation());

        // --- THIS NOW WORKS ---
        ride.setPickupLat(request.getPickupLat());
        ride.setPickupLon(request.getPickupLon());
        // ----------------------

        ride.setFare(request.getFare());
        ride.setDistanceKm(request.getDistanceKm());
        ride.setStatus("REQUESTED");

        Ride savedRide = rideRepository.save(ride);

        // Real-time broadcast
        messagingTemplate.convertAndSend(RIDE_TOPIC, savedRide);

        return savedRide;
    }

    // ... (The rest of the file remains exactly the same as before) ...

    public List<Ride> getPendingRides() {
        return rideRepository.findByStatus("REQUESTED");
    }

    public Ride acceptRide(String rideId) {
        User driver = getCurrentUser();
        String lockKey = "ride_lock:" + rideId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);

            if (isLocked) {
                Ride ride = rideRepository.findById(rideId)
                        .orElseThrow(() -> new RuntimeException("Ride not found"));

                if (!"REQUESTED".equals(ride.getStatus())) {
                    throw new RuntimeException("Ride is no longer available");
                }

                ride.setDriverId(driver.getId());
                ride.setStatus("ACCEPTED");
                Ride savedRide = rideRepository.save(ride);

                messagingTemplate.convertAndSend(RIDE_TOPIC, savedRide);
                return savedRide;
            } else {
                throw new RuntimeException("Unable to acquire lock.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Lock interrupted");
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public Ride completeRide(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        if (!"ACCEPTED".equals(ride.getStatus())) throw new RuntimeException("Invalid status");
        ride.setStatus("COMPLETED");
        return rideRepository.save(ride);
    }

    public List<Ride> getUserRides() {
        User user = getCurrentUser();
        return rideRepository.findByUserId(user.getId());
    }
}