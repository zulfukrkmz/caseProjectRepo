package com.migros.caseProject.util;

public class GeoUtils {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    public static double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        validateCoordinate(lat1, -90, 90, "latitude");
        validateCoordinate(lat2, -90, 90, "latitude");
        validateCoordinate(lon1, -180, 180, "longitude");
        validateCoordinate(lon2, -180, 180, "longitude");

        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        double deltaLat = lat2Rad - lat1Rad;
        double deltaLon = lon2Rad - lon1Rad;
        double a = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.pow(Math.sin(deltaLon / 2), 2);
        double clampedA = Math.max(0, Math.min(1, a));
        double centralAngle = 2 * Math.atan2(Math.sqrt(clampedA), Math.sqrt(1 - clampedA));

        return EARTH_RADIUS_METERS * centralAngle;
    }

    private static void validateCoordinate(double value, double min, double max, String name) {
        if (!Double.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
        }
    }
}