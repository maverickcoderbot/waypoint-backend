package ai.waypoint.backend.geocode;

import ai.waypoint.backend.cache.CacheService;
import ai.waypoint.backend.geo.Haversine;
import ai.waypoint.backend.ratelimit.RateLimiter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Rate limit, cache provider-order candidates, then apply a request-local bias. */
@Service
public class GeocodeService {

    private final CacheService cacheService;
    private final RateLimiter rateLimiter;
    private final GeocodeClient client;
    private final GeocodeProperties properties;
    private final ObjectMapper mapper;

    public GeocodeService(CacheService cacheService, @Qualifier("geocodeRateLimiter") RateLimiter rateLimiter,
            GeocodeClient client, GeocodeProperties properties, ObjectMapper mapper) {
        this.cacheService = cacheService;
        this.rateLimiter = rateLimiter;
        this.client = client;
        this.properties = properties;
        this.mapper = mapper;
    }

    /** Return candidates ordered by distance when both bias coordinates are supplied. */
    public List<GeocodeResult> query(String query, Double lat, Double lon, String clientIp) {
        rateLimiter.check(clientIp);
        if (query == null || query.isBlank()) {
            throw new InvalidQueryException("q must not be blank");
        }
        if ((lat != null && (!Double.isFinite(lat) || Math.abs(lat) > 90))
                || (lon != null && (!Double.isFinite(lon) || Math.abs(lon) > 180))) {
            throw new InvalidQueryException("Bias coordinates must be finite and in range");
        }
        String key = cacheKey(query);
        Optional<String> cached = cacheService.get(key);
        List<GeocodeResult> results;
        if (cached.isPresent()) {
            results = mapper.readValue(cached.get(), new TypeReference<List<GeocodeResult>>() { });
        } else {
            results = client.fetch(query.strip());
            cacheService.put(key, mapper.writeValueAsString(results), properties.getTtl());
        }
        List<GeocodeResult> ordered = new ArrayList<>(results);
        if (lat != null && lon != null) {
            ordered.sort(Comparator.comparingDouble(result ->
                    Haversine.distance(lat, lon, result.lat(), result.lon())));
        }
        return ordered;
    }

    /** Bias-independent UTF-8 SHA-256 of the normalized place query. */
    static String cacheKey(String query) {
        String normalized = query.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        try {
            return "geocode:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JDK must provide SHA-256", ex);
        }
    }
}
