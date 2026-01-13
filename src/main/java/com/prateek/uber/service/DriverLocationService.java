package com.prateek.uber.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DriverLocationService {

    private final StringRedisTemplate redisTemplate;

    // Grid granularity: 0.01 degrees ~ 1.1km
    private static final double GRID_SIZE = 0.01;
    private static final long EXPIRE_SECONDS = 300; // 5 minutes

    /**
     * Updates driver location in both the Spatial Grid (for O(1) lookup)
     * and the Geo Set (for precise distance calculations).
     */
    public void updateDriverLocation(String driverId, double lat, double lon) {
        String gridId = getGridId(lat, lon);
        String gridKey = "grid:" + gridId;

        // Add driver to the 1km grid bucket
        redisTemplate.opsForSet().add(gridKey, driverId);
        redisTemplate.expire(gridKey, EXPIRE_SECONDS, TimeUnit.SECONDS);

        // Update geospatial index
        redisTemplate.opsForGeo().add("driver_locations",
                new RedisGeoCommands.GeoLocation<>(driverId, new Point(lon, lat)));
    }

    /**
     * Retrieves all drivers currently located within the same grid cell as the user.
     * Complexity: O(1)
     */
    public Set<String> getNearbyDrivers(double lat, double lon) {
        String gridId = getGridId(lat, lon);
        return redisTemplate.opsForSet().members("grid:" + gridId);
    }

    private String getGridId(double lat, double lon) {
        int latIndex = (int) (lat / GRID_SIZE);
        int lonIndex = (int) (lon / GRID_SIZE);
        return latIndex + "_" + lonIndex;
    }
}