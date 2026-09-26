package ai.waypoint.backend.geocode;

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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class GeocodeServiceTest {

    private final CacheService cache = mock(CacheService.class);
    private final GeocodeClient client = mock(GeocodeClient.class);
    private final GeocodeProperties properties = new GeocodeProperties();
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final RateLimiter limiter = new RateLimiter(2, Duration.ofMinutes(1),
            Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    private final GeocodeService service = new GeocodeService(cache, limiter, client, properties, mapper);
    private final List<GeocodeResult> candidates = List.of(new GeocodeResult("far", 40, -91),
            new GeocodeResult("near", 38.6, -90.2));

    @Test
    void cacheHitSkipsUpstreamAndBiasSortsWithoutChangingCachedOrder() {
        String key = GeocodeService.cacheKey("park");
        String payload = mapper.writeValueAsString(candidates);
        when(cache.get(key)).thenReturn(Optional.of(payload));
        assertThat(service.query(" PARK ", 38.6, -90.2, "ip")).containsExactly(candidates.get(1), candidates.get(0));
        assertThat(service.query("park", null, null, "ip")).containsExactlyElementsOf(candidates);
        verifyNoInteractions(client);
        verify(cache, never()).put(anyString(), anyString(), any());
    }

    @Test
    void cacheMissStoresUnbiasedCandidatesWithTtl() {
        when(client.fetch("Park")).thenReturn(candidates);
        assertThat(service.query(" Park ", 38.6, -90.2, "ip")).startsWith(candidates.get(1));
        verify(cache).put(GeocodeService.cacheKey("Park"), mapper.writeValueAsString(candidates), Duration.ofDays(7));
    }

    @Test
    void rateLimitAlsoAppliesToCacheHits() {
        when(cache.get(anyString())).thenReturn(Optional.of("[]"));
        service.query("park", null, null, "ip");
        service.query("park", null, null, "ip");
        assertThatThrownBy(() -> service.query("park", null, null, "ip")).isInstanceOf(RateLimitException.class);
        verify(cache, times(2)).get(anyString());
        verifyNoInteractions(client);
    }

    @Test
    void invalidQueryOrBiasNeverReachesCache() {
        assertThatThrownBy(() -> service.query(" ", null, null, "ip")).isInstanceOf(InvalidQueryException.class);
        assertThatThrownBy(() -> service.query("park", Double.NaN, 0.0, "ip")).isInstanceOf(InvalidQueryException.class);
        verifyNoInteractions(cache, client);
    }

    @Test
    void failuresAreNotCached() {
        when(client.fetch("park")).thenThrow(new UpstreamUnavailableException("offline"));
        assertThatThrownBy(() -> service.query("park", null, null, "ip")).isInstanceOf(UpstreamUnavailableException.class);
        verify(cache, never()).put(anyString(), anyString(), any());
    }

    @Test
    void normalizedKeyIsSha256AndCaseInsensitive() {
        assertThat(GeocodeService.cacheKey(" ABC\n "))
                .isEqualTo("geocode:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(GeocodeService.cacheKey(" A\n B ")).isEqualTo(GeocodeService.cacheKey("a b"));
    }
}
