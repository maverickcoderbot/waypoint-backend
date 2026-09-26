package ai.waypoint.backend.geo;

/** Inclusive latitude/longitude bounds in degrees. */
public record Bbox(double south, double west, double north, double east) {

    /** Whether a point is on or inside these bounds. */
    public boolean contains(double lat, double lon) {
        return lat >= south && lat <= north && lon >= west && lon <= east;
    }
}
