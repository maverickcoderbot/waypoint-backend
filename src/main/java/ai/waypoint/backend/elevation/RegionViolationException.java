package ai.waypoint.backend.elevation;

/** A coordinate leaves the configured region. */
public class RegionViolationException extends RuntimeException {

    public RegionViolationException(String message) {
        super(message);
    }
}
