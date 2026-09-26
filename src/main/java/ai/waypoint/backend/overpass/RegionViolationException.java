package ai.waypoint.backend.overpass;

/** A query has no recognized coordinates or leaves the configured region. */
public class RegionViolationException extends RuntimeException {

    public RegionViolationException(String message) {
        super(message);
    }
}
