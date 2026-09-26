package ai.waypoint.backend.geocode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Browser-facing geocode proxy with first-forwarded-hop client quotas. */
@RestController
@RequestMapping("/api/geocode")
public class GeocodeController {

    private final GeocodeService service;

    public GeocodeController(GeocodeService service) {
        this.service = service;
    }

    /** The ingress proxy must sanitize X-Forwarded-For to identify clients reliably. */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<GeocodeResult> query(@RequestParam("q") @NotBlank String q,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lon", required = false) Double lon,
            HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr() : forwarded.split(",", 2)[0].trim();
        return service.query(q.strip(), lat, lon, ip);
    }
}
