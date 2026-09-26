package ai.waypoint.backend.elevation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ai.waypoint.backend.ratelimit.RateLimitException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Standalone MVC checks without a Spring context, database, or network. */
class ElevationControllerTest {

    private final ElevationService service = mock(ElevationService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ElevationController(service))
            .setControllerAdvice(new ElevationExceptionHandler()).build();

    @Test
    void returnsJsonAndUsesRemoteAddress() throws Exception {
        when(service.query("38.6", "-90.2", "127.0.0.1")).thenReturn("{\"elevation\":[123]}");
        mvc.perform(get("/api/elevation").param("latitude", "38.6").param("longitude", "-90.2"))
                .andExpect(status().isOk()).andExpect(content().json("{\"elevation\":[123]}"));
        verify(service).query("38.6", "-90.2", "127.0.0.1");
    }

    @Test
    void usesFirstForwardedHop() throws Exception {
        when(service.query("38.6", "-90.2", "192.0.2.1")).thenReturn("{\"elevation\":[123]}");
        mvc.perform(get("/api/elevation").param("latitude", "38.6").param("longitude", "-90.2")
                        .header("X-Forwarded-For", " 192.0.2.1, 192.0.2.2"))
                .andExpect(status().isOk());
        verify(service).query("38.6", "-90.2", "192.0.2.1");
    }

    @Test
    void mapsRateLimitToSmallJsonError() throws Exception {
        when(service.query("38.6", "-90.2", "127.0.0.1")).thenThrow(new RateLimitException("Rate limit exceeded"));
        mvc.perform(get("/api/elevation").param("latitude", "38.6").param("longitude", "-90.2")).andExpect(status().is(429))
                .andExpect(content().json("{\"error\":\"Rate limit exceeded\"}"));
    }

    @Test
    void mapsUpstreamFailureToSmallJsonError() throws Exception {
        when(service.query("38.6", "-90.2", "127.0.0.1")).thenThrow(new UpstreamUnavailableException("offline"));
        mvc.perform(get("/api/elevation").param("latitude", "38.6").param("longitude", "-90.2")).andExpect(status().isBadGateway())
                .andExpect(content().json("{\"error\":\"offline\"}"));
    }

    @Test
    void mapsInvalidQueryTo422() throws Exception {
        when(service.query("38.6", "-90.2", "127.0.0.1")).thenThrow(new InvalidQueryException("invalid"));
        mvc.perform(get("/api/elevation").param("latitude", "38.6").param("longitude", "-90.2")).andExpect(status().is(422))
                .andExpect(content().json("{\"error\":\"invalid\"}"));
    }

    @Test
    void mapsRegionViolationTo422() throws Exception {
        when(service.query("38.6", "-90.2", "127.0.0.1")).thenThrow(new RegionViolationException("outside"));
        mvc.perform(get("/api/elevation").param("latitude", "38.6").param("longitude", "-90.2"))
                .andExpect(status().is(422)).andExpect(content().json("{\"error\":\"outside\"}"));
    }

    @Test
    void missingListsReachValidationAndReturn422() throws Exception {
        when(service.query(null, null, "127.0.0.1")).thenThrow(new InvalidQueryException("required"));
        mvc.perform(get("/api/elevation")).andExpect(status().is(422))
                .andExpect(content().json("{\"error\":\"required\"}"));
    }
}
