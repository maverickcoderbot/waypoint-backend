package ai.waypoint.backend.requestlog;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link RequestLog}. */
public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {

    /** Count of requests from a client since a cutoff — the basis for rate limiting. */
    long countByClientIdAndCreatedAtAfter(String clientId, Instant since);
}
