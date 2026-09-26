package ai.waypoint.backend.geocode;

/** The upstream request could not complete successfully. */
public class UpstreamUnavailableException extends RuntimeException {

    public UpstreamUnavailableException(String message) {
        super(message);
    }
}
