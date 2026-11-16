package com.migros.caseProject.util;

import org.springframework.stereotype.Component;

@Component
public class HaversineDistanceCalculator implements DistanceCalculator {

    @Override
    public double calculateDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
        return GeoUtils.calculateDistanceMeters(lat1, lng1, lat2, lng2);
    }
}
