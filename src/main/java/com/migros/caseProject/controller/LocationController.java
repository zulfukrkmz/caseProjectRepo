package com.migros.caseProject.controller;

import com.migros.caseProject.model.data.LocationDTO;
import com.migros.caseProject.service.LocationService;
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
    public ResponseEntity<Void> receiveLocation(@RequestBody LocationDTO locationDTO) {
        locationService.processLocation(locationDTO);
        return ResponseEntity.ok().build();
    }

    // Belirli kurye için toplam mesafe
    @GetMapping("/{courierId}/total-distance")
    public ResponseEntity<Double> getTotalDistance(@PathVariable String courierId) {
        Double distance = locationService.getTotalTravelDistance(courierId);
        return ResponseEntity.ok(distance);
    }
}