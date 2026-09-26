package ai.waypoint.backend.overpass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ai.waypoint.backend.cache.CacheService;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OverpassServiceTest {

    private static final String QUERY = "node(around:100,38.6,-90.3);out;";
    private static final String IP = "192.0.2.1";
    private static final String PAYLOAD = "{\"elements\":[]}";

    @Mock private CacheService cache;
    @Mock private RateLimiter limiter;
    @Mock private OverpassClient client;
    @Mock private OverpassProperties properties;
    @InjectMocks private OverpassService service;

    private void allowRegion() {
        when(properties.getBbox()).thenReturn(new GeoFence.Bbox(38.35, -90.9, 39.05, -89.95));
    }

    @Test
    void cacheHitSkipsUpstreamAfterRateAndRegionChecks() {
        allowRegion();
        String key = OverpassService.cacheKey(QUERY);
        when(cache.get(key)).thenReturn(Optional.of(PAYLOAD));
        assertThat(service.query(QUERY, IP)).isEqualTo(PAYLOAD);
        var order = inOrder(limiter, properties, cache);
        order.verify(limiter).check(IP);
        order.verify(properties).getBbox();
        order.verify(cache).get(key);
        verifyNoInteractions(client);
        verifyNoMoreInteractions(cache);
    }

    @Test
    void cacheMissFetchesThenCachesWithConfiguredTtl() {
        allowRegion();
        String key = OverpassService.cacheKey(QUERY);
        when(cache.get(key)).thenReturn(Optional.empty());
        when(client.fetch(QUERY)).thenReturn(PAYLOAD);
        when(properties.getTtl()).thenReturn(Duration.ofHours(24));
        assertThat(service.query(QUERY, IP)).isEqualTo(PAYLOAD);
        var order = inOrder(limiter, cache, client);
        order.verify(limiter).check(IP);
        order.verify(cache).get(key);
        order.verify(client).fetch(QUERY);
        order.verify(cache).put(key, PAYLOAD, Duration.ofHours(24));
    }

    @Test
    void regionViolationShortCircuitsCacheAndUpstream() {
        allowRegion();
        assertThatThrownBy(() -> service.query("node(around:100,40,-90);", IP))
                .isInstanceOf(RegionViolationException.class);
        var order = inOrder(limiter, properties);
        order.verify(limiter).check(IP);
        order.verify(properties).getBbox();
        verifyNoInteractions(cache, client);
    }

    @Test
    void rateLimitBlocksBeforeAllOtherCollaborators() {
        doThrow(new RateLimitException("Rate limit exceeded")).when(limiter).check(IP);
        assertThatThrownBy(() -> service.query(QUERY, IP)).isInstanceOf(RateLimitException.class);
        verifyNoInteractions(properties, cache, client);
    }

    @Test
    void upstreamFailureIsNotCached() {
        allowRegion();
        String key = OverpassService.cacheKey(QUERY);
        when(cache.get(key)).thenReturn(Optional.empty());
        when(client.fetch(QUERY)).thenThrow(new UpstreamUnavailableException("All mirrors failed"));
        assertThatThrownBy(() -> service.query(QUERY, IP)).isInstanceOf(UpstreamUnavailableException.class);
        org.mockito.Mockito.verify(cache).get(key);
        verifyNoMoreInteractions(cache);
    }

    @Test
    void cacheKeyUsesNormalizedQueryAndSha256() {
        assertThat(OverpassService.cacheKey("  abc\n "))
                .isEqualTo("overpass:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(OverpassService.cacheKey(" a\n  b ")).isEqualTo(OverpassService.cacheKey("a b"));
    }
}
