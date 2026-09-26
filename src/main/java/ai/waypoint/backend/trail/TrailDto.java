package ai.waypoint.backend.trail;

import java.util.List;

/**
 * API representation of a {@link Trail}.
 *
 * <p>{@code coordinates} is an ordered list of {@code [lng, lat]} pairs (GeoJSON
 * order), ready for Leaflet to draw a polyline without further transformation.
 */
public record TrailDto(
        Long id,
        Long osmId,
        String name,
        Double lengthM,
        String difficulty,
        String type,
        List<double[]> coordinates) {
}
