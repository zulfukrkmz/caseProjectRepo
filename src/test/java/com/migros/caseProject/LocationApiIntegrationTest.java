package com.migros.caseProject;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LocationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void postReturnsTrackingResultsAndGetReturnsTheCourierTotal() throws Exception {
        String courierId = "api-test-" + UUID.randomUUID();
        String request = """
                {
                  "courierId": "%s",
                  "lat": 40.9923307,
                  "lng": 29.1244229,
                  "timestamp": 1800000000000
                }
                """.formatted(courierId);

        mockMvc.perform(post("/locations/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierId").value(courierId))
                .andExpect(jsonPath("$.segmentDistanceMeters").value(0))
                .andExpect(jsonPath("$.totalDistanceMeters").value(0))
                .andExpect(jsonPath("$.storeEntrances[0].storeName").value("Ataşehir MMM Migros"))
                .andExpect(jsonPath("$.storeEntrances[0].distanceMeters").value(0));

        mockMvc.perform(get("/locations/{courierId}/total-distance", courierId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierId").value(courierId))
                .andExpect(jsonPath("$.totalDistanceMeters").value(0));
    }

    @Test
    void tracksIndependentJourneysForTwoCouriers() throws Exception {
        String courierOne = "journey-ali-" + UUID.randomUUID();
        assertLocation(courierOne, 40.9923307, 29.1244229, 1_800_000_000_000L,
                0.0, 0.0, "Ataşehir MMM Migros");
        assertLocation(courierOne, 40.9933307, 29.1244229, 1_800_000_001_000L,
                111.19492664568354, 111.19492664568354, null);
        assertLocation(courierOne, 40.986106, 29.1161293, 1_800_000_002_000L,
                1062.9841874327753, 1174.179114078459, "Novada MMM Migros");
        assertLocation(courierOne, 40.986606, 29.1161293, 1_800_000_003_000L,
                55.59746332284177, 1229.7765774013008, null);
        assertLocation(courierOne, 40.9923307, 29.1244229, 1_800_000_060_000L,
                943.278024202827, 2173.054601604128, "Ataşehir MMM Migros");

        String courierTwo = "journey-ayse-" + UUID.randomUUID();
        assertLocation(courierTwo, 41.0066851, 28.6552262, 1_800_000_100_000L,
                0.0, 0.0, "Beylikdüzü 5M Migros");
        assertLocation(courierTwo, 41.0056851, 28.6552262, 1_800_000_101_000L,
                111.19492664426889, 111.19492664426889, null);
        assertLocation(courierTwo, 41.0056851, 28.6562262, 1_800_000_102_000L,
                83.91263758393364, 195.10756422820253, null);
        assertLocation(courierTwo, 41.055783, 29.0210292, 1_800_000_103_000L,
                31102.843912293658, 31297.95147652186, "Ortaköy MMM Migros");
        assertLocation(courierTwo, 41.056783, 29.0210292, 1_800_000_104_000L,
                111.1949266449762, 31409.146403166837, null);
        assertLocation(courierTwo, 41.055783, 29.0210292, 1_800_000_163_000L,
                111.1949266449762, 31520.341329811814, "Ortaköy MMM Migros");

        mockMvc.perform(get("/locations/{courierId}/total-distance", courierOne))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDistanceMeters").value(2173.054601604128));
        mockMvc.perform(get("/locations/{courierId}/total-distance", courierTwo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDistanceMeters").value(31520.341329811814));
    }

    @Test
    void postReturnsReadableValidationErrorForInvalidCoordinates() throws Exception {
        mockMvc.perform(post("/locations/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courierId": "invalid-coordinate",
                                  "lat": 91,
                                  "lng": 29,
                                  "timestamp": 1800000000000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("lat")));
    }

    @Test
    void postReturnsConflictForAnOutOfOrderCourierTimestamp() throws Exception {
        String courierId = "timestamp-test-" + UUID.randomUUID();
        String firstRequest = """
                {
                  "courierId": "%s",
                  "lat": 40.9923307,
                  "lng": 29.1244229,
                  "timestamp": 2000
                }
                """.formatted(courierId);
        String olderRequest = firstRequest.replace("\"timestamp\": 2000", "\"timestamp\": 1999");

        mockMvc.perform(post("/locations/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequest))
                .andExpect(status().isOk());

        mockMvc.perform(post("/locations/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(olderRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(courierId)));
    }

    @Test
    void postReturnsConflictForEqualTimestampWithoutChangingCourierState() throws Exception {
        String courierId = "equal-timestamp-test-" + UUID.randomUUID();
        String firstRequest = """
                {
                  "courierId": "%s",
                  "lat": 40.9923307,
                  "lng": 29.1244229,
                  "timestamp": 2000
                }
                """.formatted(courierId);
        String equalTimestampRequest = firstRequest
                .replace("\"lat\": 40.9923307", "\"lat\": 40.9933307");

        mockMvc.perform(post("/locations/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequest))
                .andExpect(status().isOk());

        mockMvc.perform(post("/locations/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(equalTimestampRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(courierId)));

        mockMvc.perform(get("/locations/{courierId}/total-distance", courierId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalDistanceMeters").value(0));
    }

    @Test
    void postAcceptsCoordinateAndTimestampBoundaries() throws Exception {
        double[][] coordinateBoundaries = {{90, 180}, {-90, -180}, {90, -180}, {-90, 180}};

        for (int i = 0; i < coordinateBoundaries.length; i++) {
            String courierId = "x".repeat(99) + i;
            long timestamp = i == coordinateBoundaries.length - 1 ? Long.MAX_VALUE : 0;
            mockMvc.perform(post("/locations/track")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "courierId": "%s",
                                      "lat": %s,
                                      "lng": %s,
                                      "timestamp": %d
                                    }
                                    """.formatted(courierId, coordinateBoundaries[i][0],
                                    coordinateBoundaries[i][1], timestamp)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.segmentDistanceMeters").value(0));
        }
    }

    @Test
    void postRejectsMalformedJsonAndInvalidOrMissingFields() throws Exception {
        String[] invalidRequests = {
                "{",
                """
                {"courierId":" ","lat":0,"lng":0,"timestamp":0}
                """,
                """
                {"courierId":"%s","lat":null,"lng":0,"timestamp":0}
                """.formatted(UUID.randomUUID()),
                """
                {"courierId":"%s","lat":0,"lng":-180.0001,"timestamp":0}
                """.formatted(UUID.randomUUID()),
                """
                {"courierId":"%s","lat":0,"lng":0,"timestamp":-1}
                """.formatted(UUID.randomUUID()),
                """
                {"courierId":"%s","lat":0,"lng":0}
                """.formatted(UUID.randomUUID()),
                """
                {"courierId":"%s","lat":0,"lng":0,"timestamp":0}
                """.formatted("x".repeat(101))
        };

        for (String request : invalidRequests) {
            mockMvc.perform(post("/locations/track")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(request))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }
    }

    private void assertLocation(String courierId, double lat, double lng, long timestamp,
                                double segmentDistance, double totalDistance, String storeEntrance)
            throws Exception {
        String request = """
                {
                  "courierId": "%s",
                  "lat": %s,
                  "lng": %s,
                  "timestamp": %d
                }
                """.formatted(courierId, lat, lng, timestamp);

        var result = mockMvc.perform(post("/locations/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courierId").value(courierId))
                .andExpect(jsonPath("$.segmentDistanceMeters").value(segmentDistance))
                .andExpect(jsonPath("$.totalDistanceMeters").value(totalDistance))
                .andExpect(jsonPath("$.storeEntrances.length()").value(storeEntrance == null ? 0 : 1));

        if (storeEntrance != null) {
            result.andExpect(jsonPath("$.storeEntrances[0].storeName").value(storeEntrance));
        }
    }
}
