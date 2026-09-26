package ai.waypoint.backend.overpass;

import ai.waypoint.backend.ratelimit.RateLimitException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Small JSON error responses scoped to the Overpass endpoint. */
@RestControllerAdvice(assignableTypes = OverpassController.class)
public class OverpassExceptionHandler {

    /** Coordinates outside the supported region, or no coordinates at all. */
    @ExceptionHandler(RegionViolationException.class)
    public ResponseEntity<Map<String, String>> regionViolation(RegionViolationException ex) {
        return ResponseEntity.status(422).body(Map.of("error", ex.getMessage()));
    }

    /** Per-client quota exhausted. */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, String>> rateLimit(RateLimitException ex) {
        return ResponseEntity.status(429).body(Map.of("error", ex.getMessage()));
    }

    /** Every mirror failed to provide a successful response. */
    @ExceptionHandler(UpstreamUnavailableException.class)
    public ResponseEntity<Map<String, String>> upstreamUnavailable(UpstreamUnavailableException ex) {
        return ResponseEntity.status(502).body(Map.of("error", ex.getMessage()));
    }
}
