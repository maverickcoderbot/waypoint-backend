package ai.waypoint.backend.elevation;

import ai.waypoint.backend.cache.CacheService;
import ai.waypoint.backend.ratelimit.RateLimiter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/** Rate limit, validate and geofence every sample, then cache raw elevation JSON. */
@Service
public class ElevationService {

    private final CacheService cacheService;
    private final RateLimiter rateLimiter;
    private final ElevationClient client;
    private final ElevationProperties properties;

    public ElevationService(CacheService cacheService, @Qualifier("elevationRateLimiter") RateLimiter rateLimiter,
            ElevationClient client, ElevationProperties properties) {
        this.cacheService = cacheService;
        this.rateLimiter = rateLimiter;
        this.client = client;
        this.properties = properties;
    }

    /** Execute a batch on behalf of one client, including quotas on cache hits. */
    public String query(String latitude, String longitude, String clientIp) {
        rateLimiter.check(clientIp);
        double[] lat = parse(latitude, 90);
        double[] lon = parse(longitude, 180);
        if (lat.length != lon.length) {
            throw new InvalidQueryException("Coordinate lists must have equal length");
        }
        for (int i = 0; i < lat.length; i++) {
            if (!properties.getBbox().contains(lat[i], lon[i])) {
                throw new RegionViolationException("Coordinate is outside the configured region");
            }
        }
        String normalizedLat = normalize(lat);
        String normalizedLon = normalize(lon);
        String key = cacheKey(normalizedLat, normalizedLon);
        Optional<String> cached = cacheService.get(key);
        if (cached.isPresent()) {
            return cached.get();
        }
        String payload = client.fetch(normalizedLat, normalizedLon);
        cacheService.put(key, payload, properties.getTtl());
        return payload;
    }

    private double[] parse(String csv, double limit) {
        if (csv == null || csv.isBlank()) {
            throw new InvalidQueryException("Both coordinate lists are required");
        }
        String[] values = csv.split(",", -1);
        if (values.length > properties.getMaxPoints()) {
            throw new InvalidQueryException("Too many coordinate samples");
        }
        double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            try {
                result[i] = Double.parseDouble(values[i].strip());
            } catch (NumberFormatException ex) {
                throw new InvalidQueryException("Coordinates must be finite numbers in range");
            }
            if (!Double.isFinite(result[i]) || Math.abs(result[i]) > limit) {
                throw new InvalidQueryException("Coordinates must be finite numbers in range");
            }
        }
        return result;
    }

    private static String normalize(double[] values) {
        return Arrays.stream(values).mapToObj(value -> Double.toString(value == 0 ? 0 : value))
                .collect(Collectors.joining(","));
    }

    /** UTF-8 SHA-256 of normalized latitude and longitude lists, preserving sample order. */
    static String cacheKey(String latitude, String longitude) {
        try {
            return "elevation:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((latitude + "|" + longitude).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JDK must provide SHA-256", ex);
        }
    }
}
