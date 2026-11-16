package com.migros.caseProject.controller;

import com.migros.caseProject.model.data.LocationDTO;
import com.migros.caseProject.model.data.LocationTrackingResponse;
import com.migros.caseProject.model.data.TotalDistanceResponse;
import com.migros.caseProject.service.LocationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/locations")
public class LocationController {

    private final LocationService locationService;

    // Constructor Injection
    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @PostMapping("/track")
    public ResponseEntity<LocationTrackingResponse> receiveLocation(@Valid @RequestBody LocationDTO locationDTO) {
        return ResponseEntity.ok(locationService.processLocation(locationDTO));
    }

    @GetMapping("/{courierId}/total-distance")
    public ResponseEntity<TotalDistanceResponse> getTotalDistance(@PathVariable String courierId) {
        Double distance = locationService.getTotalTravelDistance(courierId);
        return ResponseEntity.ok(new TotalDistanceResponse(courierId, distance));
    }
}