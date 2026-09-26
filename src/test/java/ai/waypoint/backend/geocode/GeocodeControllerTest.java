package ai.waypoint.backend.geocode;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ai.waypoint.backend.ratelimit.RateLimitException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Standalone MVC checks without a Spring context, database, or network. */
class GeocodeControllerTest {

    private final GeocodeService service = mock(GeocodeService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new GeocodeController(service))
            .setControllerAdvice(new GeocodeExceptionHandler()).build();

    @Test
    void returnsJsonAndUsesRemoteAddress() throws Exception {
        when(service.query("park", null, null, "127.0.0.1")).thenReturn(java.util.List.of(new GeocodeResult("park", 38.6, -90.2)));
        mvc.perform(get("/api/geocode").param("q", "park"))
                .andExpect(status().isOk()).andExpect(content().json("[{\"name\":\"park\",\"lat\":38.6,\"lon\":-90.2}]"));
        verify(service).query("park", null, null, "127.0.0.1");
    }

    @Test
    void usesFirstForwardedHop() throws Exception {
        when(service.query("park", null, null, "192.0.2.1")).thenReturn(java.util.List.of(new GeocodeResult("park", 38.6, -90.2)));
        mvc.perform(get("/api/geocode").param("q", "park")
                        .header("X-Forwarded-For", " 192.0.2.1, 192.0.2.2"))
                .andExpect(status().isOk());
        verify(service).query("park", null, null, "192.0.2.1");
    }

    @Test
    void mapsRateLimitToSmallJsonError() throws Exception {
        when(service.query("park", null, null, "127.0.0.1")).thenThrow(new RateLimitException("Rate limit exceeded"));
        mvc.perform(get("/api/geocode").param("q", "park")).andExpect(status().is(429))
                .andExpect(content().json("{\"error\":\"Rate limit exceeded\"}"));
    }

    @Test
    void mapsUpstreamFailureToSmallJsonError() throws Exception {
        when(service.query("park", null, null, "127.0.0.1")).thenThrow(new UpstreamUnavailableException("offline"));
        mvc.perform(get("/api/geocode").param("q", "park")).andExpect(status().isBadGateway())
                .andExpect(content().json("{\"error\":\"offline\"}"));
    }

    @Test
    void mapsInvalidQueryTo422() throws Exception {
        when(service.query("park", null, null, "127.0.0.1")).thenThrow(new InvalidQueryException("invalid"));
        mvc.perform(get("/api/geocode").param("q", "park")).andExpect(status().is(422))
                .andExpect(content().json("{\"error\":\"invalid\"}"));
    }

    @Test
    void missingBlankAndNonNumericParametersHaveSmallErrors() throws Exception {
        mvc.perform(get("/api/geocode")).andExpect(status().is(422)).andExpect(jsonPath("$.error").exists());
        mvc.perform(get("/api/geocode").param("q", " ")).andExpect(status().is(422))
                .andExpect(jsonPath("$.error").exists());
        mvc.perform(get("/api/geocode").param("q", "park").param("lat", "bad"))
                .andExpect(status().is(422)).andExpect(jsonPath("$.error").exists());
        verifyNoInteractions(service);
    }

    @Test
    void trimsQueryAndPassesBias() throws Exception {
        when(service.query("park", 38.6, -90.2, "127.0.0.1")).thenReturn(java.util.List.of());
        mvc.perform(get("/api/geocode").param("q", " park ").param("lat", "38.6").param("lon", "-90.2"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).query("park", 38.6, -90.2, "127.0.0.1");
    }
}
