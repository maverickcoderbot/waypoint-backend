package ai.waypoint.backend.cache;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A cached upstream response (Overpass, geocoding, elevation, photos).
 *
 * <p>Backs the "frontier" and passthrough caches: on a miss we call the upstream
 * provider, store the JSON payload here keyed by request, and serve subsequent
 * reads from Postgres until {@link #expiresAt}. Maps to the {@code api_cache} table.
 */
@Entity
@Table(name = "api_cache")
public class ApiCache {

    /** Stable request key, e.g. {@code overpass:trails:lat,lng,radius,type}. */
    @Id
    @Column(name = "cache_key")
    private String cacheKey;

    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected ApiCache() {
        // for JPA
    }

    public ApiCache(String cacheKey, String payload, Instant fetchedAt, Instant expiresAt) {
        this.cacheKey = cacheKey;
        this.payload = payload;
        this.fetchedAt = fetchedAt;
        this.expiresAt = expiresAt;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(Instant fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}
