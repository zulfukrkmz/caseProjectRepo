package com.migros.caseProject.util;

public class GeoUtils {

    // Dünya'nın yarıçapı (km olarak)
    private static final double EARTH_RADIUS = 6371.0;

    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Radyan cinsine çevir
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Haversine formülünü uygula
        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;
        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.pow(Math.sin(deltaLon / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Mesafeyi hesapla
        return EARTH_RADIUS * c; // km cinsinden mesafe döndürür
    }
}