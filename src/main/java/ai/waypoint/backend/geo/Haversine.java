package ai.waypoint.backend.geo;

/** Great-circle distances for ordering geographic candidates. */
public final class Haversine {

    private Haversine() {
    }

    /** Return distance in kilometers, including across the antimeridian. */
    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double sinLat = Math.sin(Math.toRadians(lat2 - lat1) / 2);
        double sinLon = Math.sin(Math.toRadians(lon2 - lon1) / 2);
        double a = sinLat * sinLat + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * sinLon * sinLon;
        return 6371.0 * 2 * Math.asin(Math.sqrt(Math.clamp(a, 0, 1)));
    }
}
