package ai.waypoint.backend.geocode;

import ai.waypoint.backend.ratelimit.RateLimitException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Small JSON errors scoped to the geocode endpoint. */
@RestControllerAdvice(assignableTypes = GeocodeController.class)
public class GeocodeExceptionHandler {

    /** Invalid coordinate lists or query parameters. */
    @ExceptionHandler(InvalidQueryException.class)
    public ResponseEntity<Map<String, String>> invalidQuery(InvalidQueryException ex) {
        return ResponseEntity.status(422).body(Map.of("error", ex.getMessage()));
    }

    /** Keep binding and bean-validation failures in the endpoint's small error shape. */
    @ExceptionHandler({MissingServletRequestParameterException.class,
            HandlerMethodValidationException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> invalidParameters(Exception ex) {
        return ResponseEntity.status(422).body(Map.of("error", "Invalid or missing query parameters"));
    }

    /** Per-client quota exhausted. */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<Map<String, String>> rateLimit(RateLimitException ex) {
        return ResponseEntity.status(429).body(Map.of("error", ex.getMessage()));
    }

    /** No provider could complete the request. */
    @ExceptionHandler(UpstreamUnavailableException.class)
    public ResponseEntity<Map<String, String>> upstreamUnavailable(UpstreamUnavailableException ex) {
        return ResponseEntity.status(502).body(Map.of("error", ex.getMessage()));
    }
}
