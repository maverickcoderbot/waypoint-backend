package ai.waypoint.backend.cache;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link ApiCache} entries. */
public interface ApiCacheRepository extends JpaRepository<ApiCache, String> {

    Optional<ApiCache> findByCacheKey(String cacheKey);

    @Modifying
    @Query("DELETE FROM ApiCache c WHERE c.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
