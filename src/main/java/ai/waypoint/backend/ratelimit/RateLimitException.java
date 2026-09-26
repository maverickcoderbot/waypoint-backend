package ai.waypoint.backend.ratelimit;

/** A client has exhausted its fixed-window request allowance. */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
