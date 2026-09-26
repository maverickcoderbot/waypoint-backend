package ai.waypoint.backend.overpass;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ai.waypoint.backend.ratelimit.RateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Standalone MVC tests: no Spring Boot context, database, or network. */
@ExtendWith(MockitoExtension.class)
class OverpassControllerTest {

    private static final String QUERY = "node(around:100,38.6,-90.3);out;";

    @Mock
    private OverpassService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new OverpassController(service))
                .setControllerAdvice(new OverpassExceptionHandler()).build();
    }

    @Test
    void forwardsRawQueryAndFirstForwardedHopAndReturnsRawJson() throws Exception {
        when(service.query(QUERY, "192.0.2.1")).thenReturn("{\"elements\":[]}");
        mvc.perform(post("/api/overpass").contentType(MediaType.TEXT_PLAIN).content(QUERY)
                        .header("X-Forwarded-For", " 192.0.2.1, 192.0.2.2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string("{\"elements\":[]}"));
    }

    @Test
    void fallsBackToRemoteAddress() throws Exception {
        when(service.query(QUERY, "127.0.0.1")).thenReturn("{}");
        mvc.perform(post("/api/overpass").contentType(MediaType.TEXT_PLAIN).content(QUERY))
                .andExpect(status().isOk()).andExpect(content().string("{}"));
    }

    @Test
    void mapsRegionViolationToSmallJsonError() throws Exception {
        when(service.query(QUERY, "127.0.0.1"))
                .thenThrow(new RegionViolationException("Outside region"));
        mvc.perform(post("/api/overpass").contentType(MediaType.TEXT_PLAIN).content(QUERY))
                .andExpect(status().is(422))
                .andExpect(content().json("{\"error\":\"Outside region\"}"));
    }

    @Test
    void mapsRateLimitToSmallJsonError() throws Exception {
        when(service.query(QUERY, "127.0.0.1"))
                .thenThrow(new RateLimitException("Rate limit exceeded"));
        mvc.perform(post("/api/overpass").contentType(MediaType.TEXT_PLAIN).content(QUERY))
                .andExpect(status().is(429))
                .andExpect(content().json("{\"error\":\"Rate limit exceeded\"}"));
    }

    @Test
    void mapsUpstreamFailureToSmallJsonError() throws Exception {
        when(service.query(QUERY, "127.0.0.1"))
                .thenThrow(new UpstreamUnavailableException("All mirrors failed"));
        mvc.perform(post("/api/overpass").contentType(MediaType.TEXT_PLAIN).content(QUERY))
                .andExpect(status().isBadGateway())
                .andExpect(content().json("{\"error\":\"All mirrors failed\"}"));
    }

    @Test
    void emptyBodyStillReachesService() throws Exception {
        when(service.query("", "127.0.0.1"))
                .thenThrow(new RegionViolationException("Query has no geofenceable coordinates"));
        mvc.perform(post("/api/overpass").contentType(MediaType.TEXT_PLAIN))
                .andExpect(status().is(422));
    }
}
