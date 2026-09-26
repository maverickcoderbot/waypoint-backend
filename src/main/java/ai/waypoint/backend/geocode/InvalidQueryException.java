package ai.waypoint.backend.geocode;

/** Request parameters are missing or invalid. */
public class InvalidQueryException extends RuntimeException {

    public InvalidQueryException(String message) {
        super(message);
    }
}
