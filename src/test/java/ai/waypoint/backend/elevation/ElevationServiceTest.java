package ai.waypoint.backend.elevation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ai.waypoint.backend.cache.CacheService;
import ai.waypoint.backend.ratelimit.RateLimitException;
import ai.waypoint.backend.ratelimit.RateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ElevationServiceTest {

    private final CacheService cache = mock(CacheService.class);
    private final ElevationClient client = mock(ElevationClient.class);
    private final ElevationProperties properties = new ElevationProperties();
    private final RateLimiter limiter = new RateLimiter(2, Duration.ofMinutes(1),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    private final ElevationService service = new ElevationService(cache, limiter, client, properties);

    @Test
    void missNormalizesCoordinatesAndCachesVerbatimWithTtl() {
        String body = "{ \"elevation\": [1, 2] }";
        when(client.fetch("38.6,38.7", "-90.2,-90.3")).thenReturn(body);
        assertThat(service.query(" 38.600 , 3.87e1 ", " -90.20,-90.30 ", "ip")).isEqualTo(body);
        verify(cache).put(ElevationService.cacheKey("38.6,38.7", "-90.2,-90.3"), body, Duration.ofDays(30));
    }

    @Test
    void cacheHitSkipsUpstreamAndConsumesQuota() {
        when(cache.get(ElevationService.cacheKey("38.6", "-90.2"))).thenReturn(Optional.of("{\"elevation\":[1]}"));
        assertThat(service.query("38.60", "-90.20", "ip")).isEqualTo("{\"elevation\":[1]}");
        service.query("38.6", "-90.2", "ip");
        assertThatThrownBy(() -> service.query("38.6", "-90.2", "ip")).isInstanceOf(RateLimitException.class);
        verifyNoInteractions(client);
        verify(cache, times(2)).get(anyString());
    }

    @Test
    void unequalListsAreInvalid() {
        assertThatThrownBy(() -> service.query("38.6,38.7", "-90.2", "ip"))
                .isInstanceOf(InvalidQueryException.class);
        verifyNoInteractions(cache, client);
    }

    @Test
    void missingEmptyNonFiniteAndOutOfRangeAreInvalid() {
        for (String value : new String[] {null, "", " ", "NaN", "Infinity", "91", "-91", "bad", "38.6,"}) {
            assertThatThrownBy(() -> service.query(value, "-90.2", String.valueOf(value)))
                    .isInstanceOf(InvalidQueryException.class);
        }
        assertThatThrownBy(() -> service.query("38.6", "181", "lon")).isInstanceOf(InvalidQueryException.class);
        assertThatThrownBy(() -> service.query("38.6", "NaN", "nanlon")).isInstanceOf(InvalidQueryException.class);
        verifyNoInteractions(cache, client);
    }

    @Test
    void sampleLimitIsEnforced() {
        properties.setMaxPoints(1);
        assertThatThrownBy(() -> service.query("38.6,38.7", "-90.2,-90.3", "ip"))
                .isInstanceOf(InvalidQueryException.class);
        verifyNoInteractions(cache, client);
    }

    @Test
    void everyPointIsGeofencedBeforeCacheLookup() {
        assertThatThrownBy(() -> service.query("38.6,40", "-90.2,-90.2", "ip"))
                .isInstanceOf(RegionViolationException.class);
        verifyNoInteractions(cache, client);
    }

    @Test
    void inclusiveBboxBoundaryIsAccepted() {
        when(client.fetch("38.35,39.05", "-90.9,-89.95")).thenReturn("{}");
        assertThat(service.query("38.35,39.05", "-90.9,-89.95", "ip")).isEqualTo("{}");
    }

    @Test
    void failedUpstreamIsNotCached() {
        when(client.fetch("38.6", "-90.2")).thenThrow(new UpstreamUnavailableException("offline"));
        assertThatThrownBy(() -> service.query("38.6", "-90.2", "ip")).isInstanceOf(UpstreamUnavailableException.class);
        verify(cache, never()).put(anyString(), anyString(), any());
    }
}
