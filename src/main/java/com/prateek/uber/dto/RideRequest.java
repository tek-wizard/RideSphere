package com.prateek.uber.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RideRequest {
    @NotBlank(message = "Pickup location is required")
    private String pickupLocation;

    @NotBlank(message = "Drop location is required")
    private String dropLocation;

    private double pickupLat;
    private double pickupLon;

    @NotNull(message = "Fare is required")
    private Double fare;

    @NotNull(message = "Distance is required")
    private Double distanceKm;
}