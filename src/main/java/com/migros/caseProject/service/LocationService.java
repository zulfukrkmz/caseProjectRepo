package com.migros.caseProject.service;

import com.migros.caseProject.model.CourierLocation;
import com.migros.caseProject.model.Store;
import com.migros.caseProject.model.data.LocationDTO;
import com.migros.caseProject.model.data.LocationTrackingResponse;
import com.migros.caseProject.model.data.StoreEntrance;
import com.migros.caseProject.util.DistanceCalculator;
import com.migros.caseProject.util.StoreLoader;
import com.migros.caseProject.factory.CourierLocationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private final StoreLoader storeLoader;
    private final DistanceCalculator distanceCalculator;
    private final double storeRadiusMeters;
    private final long reentryCooldownMillis;
    private final Map<String, CourierTrackingState> trackingByCourier = new ConcurrentHashMap<>();

    public LocationService(
            StoreLoader storeLoader,
            DistanceCalculator distanceCalculator,
            @Value("${migros.radius-meters:${migros.radius:100}}") double storeRadiusMeters,
            @Value("${migros.reentry-cooldown-millis:60000}") long reentryCooldownMillis) {
        this.storeLoader = storeLoader;
        this.distanceCalculator = distanceCalculator;
        this.storeRadiusMeters = storeRadiusMeters;
        this.reentryCooldownMillis = reentryCooldownMillis;
    }

    public LocationTrackingResponse processLocation(LocationDTO locationDTO) {
        CourierLocation courierLocation = CourierLocationFactory.fromDTO(locationDTO);
        CourierTrackingState state = trackingByCourier.computeIfAbsent(
                courierLocation.getCourierId(), ignored -> new CourierTrackingState());

        synchronized (state) {
            if (state.lastLocation != null
                    && courierLocation.getTimestamp() <= state.lastLocation.getTimestamp()) {
                log.warn("Rejected out-of-order location for courier {}", courierLocation.getCourierId());
                throw new InvalidLocationTimestampException(courierLocation.getCourierId());
            }

            double segmentDistanceMeters = state.lastLocation == null ? 0
                    : distanceCalculator.calculateDistanceMeters(
                            state.lastLocation.getLat(), state.lastLocation.getLng(),
                            courierLocation.getLat(), courierLocation.getLng());
            state.totalDistanceMeters += segmentDistanceMeters;
            state.lastLocation = courierLocation;

            List<StoreEntrance> entrances = detectStoreEntrances(courierLocation, state);
            return new LocationTrackingResponse(
                    courierLocation.getCourierId(),
                    segmentDistanceMeters,
                    state.totalDistanceMeters,
                    List.copyOf(entrances));
        }
    }

    public double getTotalTravelDistance(String courierId) {
        CourierTrackingState state = trackingByCourier.get(courierId);
        if (state == null) {
            return 0;
        }
        synchronized (state) {
            return state.totalDistanceMeters;
        }
    }

    private List<StoreEntrance> detectStoreEntrances(CourierLocation location, CourierTrackingState state) {
        List<StoreEntrance> entrances = new ArrayList<>();
        for (Store store : storeLoader.getStores()) {
            double distanceMeters = distanceCalculator.calculateDistanceMeters(
                    location.getLat(), location.getLng(), store.getLat(), store.getLng());
            boolean isInside = distanceMeters <= storeRadiusMeters;
            boolean wasInside = state.insideStores.getOrDefault(store.getName(), false);

            if (isInside && !wasInside) {
                Long lastCountedEntry = state.lastCountedEntryTimestamps.get(store.getName());
                if (lastCountedEntry == null
                        || location.getTimestamp() - lastCountedEntry >= reentryCooldownMillis) {
                    entrances.add(new StoreEntrance(store.getName(), distanceMeters, location.getTimestamp()));
                    state.lastCountedEntryTimestamps.put(store.getName(), location.getTimestamp());
                    log.info("Courier {} entered store '{}' at {} m",
                            location.getCourierId(), store.getName(), Math.round(distanceMeters * 100.0) / 100.0);
                } else {
                    log.info("Courier {} re-entered store '{}' within the cooldown; entrance ignored",
                            location.getCourierId(), store.getName());
                }
            }
            state.insideStores.put(store.getName(), isInside);
        }
        return entrances;
    }

    private static final class CourierTrackingState {
        private final Map<String, Boolean> insideStores = new HashMap<>();
        private final Map<String, Long> lastCountedEntryTimestamps = new HashMap<>();
        private CourierLocation lastLocation;
        private double totalDistanceMeters;
    }
}