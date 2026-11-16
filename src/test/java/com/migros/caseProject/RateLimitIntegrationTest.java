package com.migros.caseProject;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "rate-limit.capacity=1",
        "rate-limit.refill-tokens=1",
        "rate-limit.refill-interval-millis=60000"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rateLimitsLocationTrackingByClientIp() throws Exception {
        String request = """
                {
                  "courierId": "rate-limit-post-test",
                  "lat": 40.9923307,
                  "lng": 29.1244229,
                  "timestamp": 0
                }
                """;

        mockMvc.perform(withRemoteAddress(post("/locations/track")
                .contentType("application/json")
                .content(request), "192.0.2.10"))
                .andExpect(status().isOk());

        mockMvc.perform(withRemoteAddress(post("/locations/track")
                .contentType("application/json")
                .content(request), "192.0.2.10"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"message\":\"Rate limit exceeded. Please retry later.\"}"));
    }

    @Test
    void rateLimitsTotalDistanceEndpointByClientIp() throws Exception {
        mockMvc.perform(withRemoteAddress(get("/locations/rate-limit-get-test/total-distance"), "192.0.2.11"))
                .andExpect(status().isOk());

        mockMvc.perform(withRemoteAddress(get("/locations/rate-limit-get-test/total-distance"), "192.0.2.11"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"))
                .andExpect(content().json("{\"message\":\"Rate limit exceeded. Please retry later.\"}"));
    }

    private static MockHttpServletRequestBuilder withRemoteAddress(
            MockHttpServletRequestBuilder request, String remoteAddress) {
        return request.with(servletRequest -> {
            servletRequest.setRemoteAddr(remoteAddress);
            return servletRequest;
        });
    }
}
