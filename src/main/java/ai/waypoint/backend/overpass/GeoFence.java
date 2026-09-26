package ai.waypoint.backend.overpass;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure coordinate checks for the proxy's supported Overpass QL filter forms.
 *
 * <p>Scans around-filter centers and both corners of bounding-box filters. Like
 * the POC, this is a coordinate scan, not a complete Overpass parser or a check
 * that an around-filter's radius stays inside the region.
 */
public final class GeoFence {

    private static final String NUMBER = "([+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+))";
    private static final Pattern AROUND = Pattern.compile(
            "around\\s*:\\s*" + NUMBER + "\\s*,\\s*" + NUMBER + "\\s*,\\s*" + NUMBER);
    private static final Pattern BOX = Pattern.compile(
            "\\(\\s*" + NUMBER + "\\s*,\\s*" + NUMBER + "\\s*,\\s*"
                    + NUMBER + "\\s*,\\s*" + NUMBER + "\\s*\\)");

    private GeoFence() {
    }

    /** Inclusive latitude/longitude bounds in degrees. */
    public record Bbox(double south, double west, double north, double east) {

        /** Whether a point is on or inside these bounds. */
        public boolean contains(double lat, double lon) {
            return lat >= south && lat <= north && lon >= west && lon <= east;
        }
    }

    /**
     * Return the first violation, or empty when all recognized coordinates fit.
     * Queries without recognized coordinates are rejected rather than sent upstream.
     */
    public static Optional<String> regionViolation(String query, Bbox bbox) {
        boolean found = false;
        Matcher around = AROUND.matcher(query);
        while (around.find()) {
            found = true;
            if (!bbox.contains(value(around, 2), value(around, 3))) {
                return Optional.of("Around coordinate is outside the configured region");
            }
        }
        Matcher box = BOX.matcher(query);
        while (box.find()) {
            found = true;
            if (!bbox.contains(value(box, 1), value(box, 2))
                    || !bbox.contains(value(box, 3), value(box, 4))) {
                return Optional.of("Bounding box is outside the configured region");
            }
        }
        return found ? Optional.empty() : Optional.of("Query has no geofenceable coordinates");
    }

    private static double value(Matcher matcher, int group) {
        return Double.parseDouble(matcher.group(group));
    }
}
