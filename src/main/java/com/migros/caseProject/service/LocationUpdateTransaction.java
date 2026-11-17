package com.migros.caseProject.service;

import com.migros.caseProject.model.CourierLocation;
import com.migros.caseProject.model.CourierTrackingState;
import com.migros.caseProject.model.Store;
import com.migros.caseProject.model.data.LocationTrackingResponse;
import com.migros.caseProject.model.data.StoreEntrance;
import com.migros.caseProject.repository.CourierTrackingStateRepository;
import com.migros.caseProject.util.DistanceCalculator;
import com.migros.caseProject.util.StoreLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class LocationUpdateTransaction {

    private static final Logger log = LoggerFactory.getLogger(LocationUpdateTransaction.class);

    private final StoreLoader storeLoader;
    private final DistanceCalculator distanceCalculator;
    private final CourierTrackingStateRepository stateRepository;
    private final double storeRadiusMeters;
    private final long reentryCooldownMillis;

    public LocationUpdateTransaction(
            StoreLoader storeLoader,
            DistanceCalculator distanceCalculator,
            CourierTrackingStateRepository stateRepository,
            @Value("${migros.radius-meters:${migros.radius:100}}") double storeRadiusMeters,
            @Value("${migros.reentry-cooldown-millis:60000}") long reentryCooldownMillis) {
        this.storeLoader = storeLoader;
        this.distanceCalculator = distanceCalculator;
        this.stateRepository = stateRepository;
        this.storeRadiusMeters = storeRadiusMeters;
        this.reentryCooldownMillis = reentryCooldownMillis;
    }

    @Transactional
    public LocationTrackingResponse processLocation(CourierLocation courierLocation) {
        CourierTrackingState state = stateRepository.findByCourierIdForUpdate(courierLocation.getCourierId())
                .orElseThrow(() -> new IllegalStateException(
                        "Tracking state was not initialized for courier " + courierLocation.getCourierId()));

        if (state.getLastTimestamp() != null
                && courierLocation.getTimestamp() <= state.getLastTimestamp()) {
            log.warn("Rejected out-of-order location for courier {}", courierLocation.getCourierId());
            throw new InvalidLocationTimestampException(courierLocation.getCourierId());
        }

        double segmentDistanceMeters = state.getLastLatitude() == null ? 0
                : distanceCalculator.calculateDistanceMeters(
                        state.getLastLatitude(), state.getLastLongitude(),
                        courierLocation.getLat(), courierLocation.getLng());
        state.setTotalDistanceMeters(state.getTotalDistanceMeters() + segmentDistanceMeters);
        state.setLastLatitude(courierLocation.getLat());
        state.setLastLongitude(courierLocation.getLng());
        state.setLastTimestamp(courierLocation.getTimestamp());

        List<StoreEntrance> entrances = detectStoreEntrances(courierLocation, state);
        return new LocationTrackingResponse(
                courierLocation.getCourierId(),
                segmentDistanceMeters,
                state.getTotalDistanceMeters(),
                List.copyOf(entrances));
    }

    private List<StoreEntrance> detectStoreEntrances(CourierLocation location, CourierTrackingState state) {
        List<StoreEntrance> entrances = new ArrayList<>();
        for (Store store : storeLoader.getStores()) {
            double distanceMeters = distanceCalculator.calculateDistanceMeters(
                    location.getLat(), location.getLng(), store.getLat(), store.getLng());
            boolean isInside = distanceMeters <= storeRadiusMeters;
            boolean wasInside = state.getInsideStores().getOrDefault(store.getName(), false);

            if (isInside && !wasInside) {
                Long lastCountedEntry = state.getLastCountedEntryTimestamps().get(store.getName());
                if (lastCountedEntry == null
                        || location.getTimestamp() - lastCountedEntry >= reentryCooldownMillis) {
                    entrances.add(new StoreEntrance(store.getName(), distanceMeters, location.getTimestamp()));
                    state.getLastCountedEntryTimestamps().put(store.getName(), location.getTimestamp());
                    log.info("Courier {} entered store '{}' at {} m",
                            location.getCourierId(), store.getName(), Math.round(distanceMeters * 100.0) / 100.0);
                } else {
                    log.info("Courier {} re-entered store '{}' within the cooldown; entrance ignored",
                            location.getCourierId(), store.getName());
                }
            }
            state.getInsideStores().put(store.getName(), isInside);
        }
        return entrances;
    }
}
