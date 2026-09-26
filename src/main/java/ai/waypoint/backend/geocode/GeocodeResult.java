package ai.waypoint.backend.geocode;

/** Stable candidate shape independent of the upstream geocoding provider. */
public record GeocodeResult(String name, double lat, double lon) {
}
