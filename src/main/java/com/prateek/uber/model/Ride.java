package com.prateek.uber.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Document(collection = "rides")
public class Ride {
    @Id
    private String id;
    private String userId;
    private String driverId;

    // Address Strings (e.g., "Times Square")
    private String pickupLocation;
    private String dropLocation;

    // FIELDS FOR GEO-SPATIAL FEATURES
    private double pickupLat;
    private double pickupLon;

    private double fare;
    private double distanceKm;
    private String status; // REQUESTED, ACCEPTED, COMPLETED, CANCELLED
    private Date createdAt = new Date();
}