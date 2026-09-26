package ai.waypoint.backend.overpass;

import ai.waypoint.backend.cache.CacheService;
import ai.waypoint.backend.ratelimit.RateLimiter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Proxy pipeline: rate limit, geofence, persistent cache, then mirror fallback.
 * Every request consumes quota, including cache hits and geofence rejections.
 */
@Service
public class OverpassService {

    private final CacheService cacheService;
    private final RateLimiter rateLimiter;
    private final OverpassClient client;
    private final OverpassProperties properties;

    public OverpassService(CacheService cacheService, @Qualifier("overpassRateLimiter") RateLimiter rateLimiter,
            OverpassClient client, OverpassProperties properties) {
        this.cacheService = cacheService;
        this.rateLimiter = rateLimiter;
        this.client = client;
        this.properties = properties;
    }

    /** Execute a query on behalf of one client, preserving the upstream JSON body. */
    public String query(String query, String clientIp) {
        rateLimiter.check(clientIp);
        GeoFence.regionViolation(query, properties.getBbox()).ifPresent(reason -> {
            throw new RegionViolationException(reason);
        });
        String key = cacheKey(query);
        Optional<String> cached = cacheService.get(key);
        if (cached.isPresent()) {
            return cached.get();
        }
        String payload = client.fetch(query);
        cacheService.put(key, payload, properties.getTtl());
        return payload;
    }

    /** Hash a trimmed query with whitespace runs collapsed using UTF-8 SHA-256. */
    static String cacheKey(String query) {
        String normalized = query.strip().replaceAll("\\s+", " ");
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8));
            return "overpass:" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JDK must provide SHA-256", ex);
        }
    }
}
