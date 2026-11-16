package com.migros.caseProject.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeoUtilsTest {

    @Test
    void returnsZeroForTheSameCoordinate() {
        assertEquals(0, GeoUtils.calculateDistanceMeters(40.9923307, 29.1244229, 40.9923307, 29.1244229));
    }

    @Test
    void calculatesKnownOneDegreeEquatorialDistanceInMeters() {
        double distance = GeoUtils.calculateDistanceMeters(0, 0, 0, 1);

        assertEquals(111_194.93, distance, 1);
    }

    @Test
    void distanceIsSymmetric() {
        double forward = GeoUtils.calculateDistanceMeters(40.9923307, 29.1244229, 41.0066851, 28.6552262);
        double reverse = GeoUtils.calculateDistanceMeters(41.0066851, 28.6552262, 40.9923307, 29.1244229);

        assertEquals(forward, reverse, 1e-9);
    }

    @Test
    void rejectsCoordinatesOutsideValidRanges() {
        assertThrows(IllegalArgumentException.class, () -> GeoUtils.calculateDistanceMeters(91, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> GeoUtils.calculateDistanceMeters(0, Double.NaN, 0, 0));
    }
}
