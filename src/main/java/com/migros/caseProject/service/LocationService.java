package com.migros.caseProject.service;

import com.migros.caseProject.factory.CourierLocationFactory;
import com.migros.caseProject.model.CourierLocation;
import com.migros.caseProject.model.CourierTrackingState;
import com.migros.caseProject.model.data.LocationDTO;
import com.migros.caseProject.model.data.LocationTrackingResponse;
import com.migros.caseProject.repository.CourierTrackingStateRepository;
import org.springframework.stereotype.Service;

@Service
public class LocationService {

    private final CourierTrackingStateRepository stateRepository;
    private final CourierTrackingStateInitializer stateInitializer;
    private final LocationUpdateTransaction locationUpdateTransaction;

    public LocationService(
            CourierTrackingStateRepository stateRepository,
            CourierTrackingStateInitializer stateInitializer,
            LocationUpdateTransaction locationUpdateTransaction) {
        this.stateRepository = stateRepository;
        this.stateInitializer = stateInitializer;
        this.locationUpdateTransaction = locationUpdateTransaction;
    }

    public LocationTrackingResponse processLocation(LocationDTO locationDTO) {
        CourierLocation courierLocation = CourierLocationFactory.fromDTO(locationDTO);
        stateInitializer.ensureExists(courierLocation.getCourierId());
        return locationUpdateTransaction.processLocation(courierLocation);
    }

    public double getTotalTravelDistance(String courierId) {
        return stateRepository.findById(courierId)
                .map(CourierTrackingState::getTotalDistanceMeters)
                .orElse(0.0);
    }
}