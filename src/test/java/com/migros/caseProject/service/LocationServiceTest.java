package com.migros.caseProject.service;

import com.migros.caseProject.model.Store;
import com.migros.caseProject.model.data.LocationDTO;
import com.migros.caseProject.model.data.LocationTrackingResponse;
import com.migros.caseProject.util.HaversineDistanceCalculator;
import com.migros.caseProject.util.StoreLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocationServiceTest {

    private static final Store STORE = new Store("Test Migros", 0.0, 0.0);
    private static final double ONE_DEGREE_LONGITUDE_METERS = 111_194.93;
    private LocationService locationService;

    @BeforeEach
    void setUp() {
        StoreLoader storeLoader = mock(StoreLoader.class);
        when(storeLoader.getStores()).thenReturn(List.of(STORE));
        locationService = new LocationService(storeLoader, new HaversineDistanceCalculator(), 100, 60_000);
    }

    @Test
    void firstLocationHasZeroSegmentDistanceAndCountsStoreEntryWithin100Meters() {
        double latitude = latitudeForMeters(50);

        LocationTrackingResponse response = locationService.processLocation(location("courier-1", latitude, 0, 1_000));

        assertEquals(0, response.segmentDistanceMeters());
        assertEquals(0, response.totalDistanceMeters());
        assertEquals(1, response.storeEntrances().size());
        assertEquals("Test Migros", response.storeEntrances().get(0).storeName());
        assertEquals(50, response.storeEntrances().get(0).distanceMeters(), 0.01);
    }

    @Test
    void includesCourierExactlyOnTheConfiguredStoreRadius() {
        StoreLoader storeLoader = mock(StoreLoader.class);
        when(storeLoader.getStores()).thenReturn(List.of(STORE));
        LocationService service = new LocationService(storeLoader, (lat1, lng1, lat2, lng2) -> 100, 100, 60_000);

        LocationTrackingResponse response = service.processLocation(location("courier-boundary", 0, 0, 1_000));

        assertEquals(1, response.storeEntrances().size());
        assertEquals(100, response.storeEntrances().get(0).distanceMeters());
    }

    @Test
    void doesNotCountPresenceAsAnotherEntryUntilCourierLeavesTheStore() {
        LocationTrackingResponse first = locationService.processLocation(location("courier-1", latitudeForMeters(50), 0, 1_000));
        LocationTrackingResponse stillInside = locationService.processLocation(location("courier-1", latitudeForMeters(25), 0, 2_000));

        assertEquals(1, first.storeEntrances().size());
        assertEquals(0, stillInside.storeEntrances().size());
    }

    @Test
    void suppressesReentryWithinOneMinuteButCountsItAtTheExactCooldownBoundary() {
        double inside = latitudeForMeters(50);
        double outside = latitudeForMeters(150);
        locationService.processLocation(location("courier-1", inside, 0, 1_000));
        locationService.processLocation(location("courier-1", outside, 0, 2_000));
        LocationTrackingResponse suppressed = locationService.processLocation(location("courier-1", inside, 0, 60_999));

        assertEquals(0, suppressed.storeEntrances().size());

        locationService.processLocation(location("courier-1", outside, 0, 61_000));
        LocationTrackingResponse accepted = locationService.processLocation(location("courier-1", inside, 0, 61_000));

        assertEquals(1, accepted.storeEntrances().size());
    }

    @Test
    void accumulatesSegmentDistancesAndRejectsOutOfOrderLocationsWithoutChangingState() {
        locationService.processLocation(location("courier-1", 0, 0, 1_000));
        LocationTrackingResponse second = locationService.processLocation(location("courier-1", 0, 0.001, 2_000));
        double previousTotal = locationService.getTotalTravelDistance("courier-1");

        assertEquals(ONE_DEGREE_LONGITUDE_METERS / 1_000, second.segmentDistanceMeters(), 0.01);
        assertEquals(second.segmentDistanceMeters(), second.totalDistanceMeters(), 0.01);
        assertThrows(InvalidLocationTimestampException.class,
                () -> locationService.processLocation(location("courier-1", 0, 0.002, 1_999)));
        assertEquals(previousTotal, locationService.getTotalTravelDistance("courier-1"), 0.01);
    }

    private static LocationDTO location(String courierId, double lat, double lng, long timestamp) {
        return new LocationDTO(courierId, lat, lng, timestamp);
    }

    private static double latitudeForMeters(double meters) {
        return Math.toDegrees(meters / 6_371_000.0);
    }
}
