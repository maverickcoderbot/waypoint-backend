package ai.waypoint.backend.elevation;

/** Request parameters are missing or invalid. */
public class InvalidQueryException extends RuntimeException {

    public InvalidQueryException(String message) {
        super(message);
    }
}
