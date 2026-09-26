package ai.waypoint.backend.overpass;

/** No configured Overpass mirror could complete the request successfully. */
public class UpstreamUnavailableException extends RuntimeException {

    public UpstreamUnavailableException(String message) {
        super(message);
    }
}
