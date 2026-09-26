package ai.waypoint.backend.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Postgres-backed cache for upstream provider responses.
 *
 * <p>Used by the (upcoming) Overpass/geocoding/elevation/photo proxies to avoid
 * hammering shared public services: {@link #get} on the request key, and on a miss
 * the caller fetches upstream and calls {@link #put}. Expired rows are ignored on
 * read and swept by {@link #evictExpired}.
 */
@Service
public class CacheService {

    private final ApiCacheRepository repository;

    public CacheService(ApiCacheRepository repository) {
        this.repository = repository;
    }

    /** Return the cached payload for {@code key} if present and unexpired. */
    @Transactional(readOnly = true)
    public Optional<String> get(String key) {
        Instant now = Instant.now();
        return repository.findByCacheKey(key)
                .filter(entry -> !entry.isExpired(now))
                .map(ApiCache::getPayload);
    }

    /** Store (or overwrite) a payload under {@code key} with a time-to-live. */
    @Transactional
    public void put(String key, String payload, Duration ttl) {
        Instant now = Instant.now();
        ApiCache entry = repository.findByCacheKey(key).orElseGet(() -> new ApiCache());
        entry.setCacheKey(key);
        entry.setPayload(payload);
        entry.setFetchedAt(now);
        entry.setExpiresAt(now.plus(ttl));
        repository.save(entry);
    }

    /** Delete expired entries; returns the number removed. */
    @Transactional
    public int evictExpired() {
        return repository.deleteExpired(Instant.now());
    }
}
