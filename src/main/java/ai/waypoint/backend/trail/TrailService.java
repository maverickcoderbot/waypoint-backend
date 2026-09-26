package ai.waypoint.backend.trail;

import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for trail lookups.
 *
 * <p>Phase 1 of the backend serves owned data straight from PostGIS. The "frontier"
 * fallback (live Overpass on a miss, then cache + queue import) will hang off
 * {@link #findNear} once the proxy is wired in; for now a miss simply returns empty.
 */
@Service
public class TrailService {

    /** Hard ceiling on results per query, mirroring the frontend's cap. */
    private static final int MAX_RESULTS = 150;

    private final TrailRepository trailRepository;

    public TrailService(TrailRepository trailRepository) {
        this.trailRepository = trailRepository;
    }

    /**
     * Find trails near a point.
     *
     * @param lat          latitude in degrees
     * @param lng          longitude in degrees
     * @param radiusMeters search radius in metres
     * @param type         optional mode filter (walk|cycle|vehicle); {@code null} for any
     * @param limit        max results, clamped to {@value #MAX_RESULTS}
     */
    @Transactional(readOnly = true)
    public List<TrailDto> findNear(double lat, double lng, double radiusMeters, String type, int limit) {
        int cappedLimit = Math.min(Math.max(limit, 1), MAX_RESULTS);
        String normalizedType = (type == null || type.isBlank()) ? null : type.trim().toLowerCase();
        return trailRepository.findNear(lat, lng, radiusMeters, normalizedType, cappedLimit)
                .stream()
                .map(TrailService::toDto)
                .toList();
    }

    private static TrailDto toDto(Trail trail) {
        return new TrailDto(
                trail.getId(),
                trail.getOsmId(),
                trail.getName(),
                trail.getLengthM(),
                trail.getDifficulty(),
                trail.getType(),
                coordinatesOf(trail.getGeom()));
    }

    private static List<double[]> coordinatesOf(LineString geom) {
        if (geom == null) {
            return List.of();
        }
        List<double[]> out = new ArrayList<>(geom.getNumPoints());
        for (Coordinate c : geom.getCoordinates()) {
            // JTS stores x=lng, y=lat; emit in GeoJSON [lng, lat] order.
            out.add(new double[] {c.x, c.y});
        }
        return out;
    }
}
