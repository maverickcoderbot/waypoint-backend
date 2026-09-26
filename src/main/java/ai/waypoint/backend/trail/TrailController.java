package ai.waypoint.backend.trail;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Trail search endpoint.
 *
 * <p>{@code GET /api/trails?lat=&lng=&radius=&type=&limit=} — returns trails near a
 * point, nearest first. This is the single call the Waypoint PWA makes for the map;
 * the frontend only needs its base URL pointed here.
 */
@RestController
@RequestMapping("/api/trails")
@Validated
public class TrailController {

    /** Default search radius when the client omits one (7 km, matching the frontend). */
    private static final double DEFAULT_RADIUS_METERS = 7_000;

    private final TrailService trailService;

    public TrailController(TrailService trailService) {
        this.trailService = trailService;
    }

    @GetMapping
    public List<TrailDto> nearby(
            @RequestParam @DecimalMin("-90") @DecimalMax("90") double lat,
            @RequestParam @DecimalMin("-180") @DecimalMax("180") double lng,
            @RequestParam(required = false) @Min(1) @Max(50_000) Double radius,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "150") @Min(1) @Max(150) int limit) {
        double radiusMeters = (radius == null) ? DEFAULT_RADIUS_METERS : radius;
        return trailService.findNear(lat, lng, radiusMeters, type, limit);
    }
}
