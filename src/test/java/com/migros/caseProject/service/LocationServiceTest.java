package com.migros.caseProject.service;

import com.migros.caseProject.model.Store;
import com.migros.caseProject.model.data.LocationDTO;
import com.migros.caseProject.model.data.LocationTrackingResponse;
import com.migros.caseProject.repository.CourierTrackingStateRepository;
import com.migros.caseProject.util.DistanceCalculator;
import com.migros.caseProject.util.HaversineDistanceCalculator;
import com.migros.caseProject.util.StoreLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@SpringBootTest
class LocationServiceTest {

    private static final Store STORE = new Store("Test Migros", 0.0, 0.0);
    private static final double ONE_DEGREE_LONGITUDE_METERS = 111_194.93;

    @Autowired
    private LocationService locationService;

    @Autowired
    private CourierTrackingStateRepository stateRepository;

    @MockitoBean
    private StoreLoader storeLoader;

    @MockitoBean
    private DistanceCalculator distanceCalculator;

    @BeforeEach
    void setUp() {
        stateRepository.deleteAll();
        when(storeLoader.getStores()).thenReturn(List.of(STORE));
        HaversineDistanceCalculator haversine = new HaversineDistanceCalculator();
        when(distanceCalculator.calculateDistanceMeters(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenAnswer(invocation -> haversine.calculateDistanceMeters(
                        invocation.getArgument(0), invocation.getArgument(1),
                        invocation.getArgument(2), invocation.getArgument(3)));
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
        when(distanceCalculator.calculateDistanceMeters(0, 0, 0, 0)).thenReturn(100.0);

        LocationTrackingResponse response = locationService.processLocation(location("courier-boundary", 0, 0, 1_000));

        assertEquals(1, response.storeEntrances().size());
        assertEquals(100, response.storeEntrances().get(0).distanceMeters());
    }

    @Test
    void distinguishesLocationsJustInsideAndOutsideTheStoreRadius() {
        double justInside = latitudeForMeters(99.99);
        double justOutside = latitudeForMeters(100.01);

        LocationTrackingResponse inside = locationService.processLocation(
                location("courier-inside", justInside, 0, 1_000));
        LocationTrackingResponse outside = locationService.processLocation(
                location("courier-outside", justOutside, 0, 1_000));

        assertEquals(1, inside.storeEntrances().size());
        assertEquals(0, outside.storeEntrances().size());
    }

    @Test
    void doesNotCountPresenceAsAnotherEntryUntilCourierLeavesTheStore() {
        LocationTrackingResponse first = locationService.processLocation(location("courier-1", latitudeForMeters(50), 0, 1_000));
        LocationTrackingResponse stillInside = locationService.processLocation(location("courier-1", latitudeForMeters(25), 0, 2_000));

        assertEquals(1, first.storeEntrances().size());
        assertEquals(0, stillInside.storeEntrances().size());
    }

    @Test
    void suppressesReentryJustBeforeCooldownAndCountsItAtAndAfterTheBoundary() {
        double inside = latitudeForMeters(50);
        double outside = latitudeForMeters(150);
        locationService.processLocation(location("courier-before", inside, 0, 1_000));
        locationService.processLocation(location("courier-before", outside, 0, 1_001));
        LocationTrackingResponse suppressed = locationService.processLocation(
                location("courier-before", inside, 0, 60_999));
        assertEquals(0, suppressed.storeEntrances().size());

        locationService.processLocation(location("courier-exact", inside, 0, 1_000));
        locationService.processLocation(location("courier-exact", outside, 0, 1_001));
        LocationTrackingResponse atBoundary = locationService.processLocation(
                location("courier-exact", inside, 0, 61_000));
        assertEquals(1, atBoundary.storeEntrances().size());

        locationService.processLocation(location("courier-after", inside, 0, 1_000));
        locationService.processLocation(location("courier-after", outside, 0, 1_001));
        LocationTrackingResponse afterBoundary = locationService.processLocation(
                location("courier-after", inside, 0, 61_001));
        assertEquals(1, afterBoundary.storeEntrances().size());
    }

    @Test
    void rejectsEqualTimestampWithoutChangingTheLastLocationOrTotal() {
        locationService.processLocation(location("courier-1", 0, 0, 1_000));
        double firstSegment = locationService.processLocation(
                location("courier-1", 0, 0.001, 2_000)).segmentDistanceMeters();
        double previousTotal = locationService.getTotalTravelDistance("courier-1");

        assertEquals(ONE_DEGREE_LONGITUDE_METERS / 1_000, firstSegment, 0.01);
        assertThrows(InvalidLocationTimestampException.class,
                () -> locationService.processLocation(location("courier-1", 0, 0.002, 2_000)));
        assertEquals(previousTotal, locationService.getTotalTravelDistance("courier-1"), 0.01);

        LocationTrackingResponse next = locationService.processLocation(
                location("courier-1", 0, 0.002, 2_001));
        assertEquals(ONE_DEGREE_LONGITUDE_METERS / 1_000, next.segmentDistanceMeters(), 0.01);
        assertEquals(firstSegment + next.segmentDistanceMeters(), next.totalDistanceMeters(), 0.01);
    }

    @Test
    void keepsTrackingStateIndependentForEachCourier() {
        locationService.processLocation(location("courier-1", 0, 0, 1_000));
        locationService.processLocation(location("courier-1", 0, 0.001, 2_000));

        LocationTrackingResponse firstLocationForSecondCourier = locationService.processLocation(
                location("courier-2", 0, 0.002, 1_000));

        assertEquals(0, firstLocationForSecondCourier.segmentDistanceMeters());
        assertEquals(0, firstLocationForSecondCourier.totalDistanceMeters());
        assertEquals(0, locationService.getTotalTravelDistance("unknown-courier"));
        assertEquals(ONE_DEGREE_LONGITUDE_METERS / 1_000,
                locationService.getTotalTravelDistance("courier-1"), 0.01);
    }

    @Test
    void countsEntriesForEveryStoreWhenOneLocationIsInsideMultipleStores() {
        when(storeLoader.getStores()).thenReturn(List.of(
                new Store("Store A", 0.0, 0.0),
                new Store("Store B", 0.0, 0.0)));

        LocationTrackingResponse response = locationService.processLocation(location("courier-multi-store", 0, 0, 1_000));

        assertEquals(2, response.storeEntrances().size());
        assertEquals(List.of("Store A", "Store B"),
                response.storeEntrances().stream().map(entrance -> entrance.storeName()).toList());
    }

    @Test
    void acceptsOnlyOneOfConcurrentLocationsWithTheSameCourierTimestamp() throws Exception {
        int requestCount = 12;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (int i = 0; i < requestCount; i++) {
                double latitude = latitudeForMeters(i);
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to start concurrent requests");
                    }
                    try {
                        locationService.processLocation(location("courier-concurrent", latitude, 0, 1_000));
                        return true;
                    } catch (InvalidLocationTimestampException exception) {
                        return false;
                    }
                }));
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            int acceptedCount = 0;
            for (Future<Boolean> result : results) {
                if (result.get(10, TimeUnit.SECONDS)) {
                    acceptedCount++;
                }
            }
            assertEquals(1, acceptedCount);
            assertEquals(0, locationService.getTotalTravelDistance("courier-concurrent"));
        } finally {
            executor.shutdownNow();
        }
    }

    private static LocationDTO location(String courierId, double lat, double lng, long timestamp) {
        return new LocationDTO(courierId, lat, lng, timestamp);
    }

    private static double latitudeForMeters(double meters) {
        return Math.toDegrees(meters / 6_371_000.0);
    }
}
