package ai.waypoint.backend.elevation;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Browser-facing elevation proxy with first-forwarded-hop client quotas. */
@RestController
@RequestMapping("/api/elevation")
public class ElevationController {

    private final ElevationService service;

    public ElevationController(ElevationService service) {
        this.service = service;
    }

    /** The ingress proxy must sanitize X-Forwarded-For to identify clients reliably. */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public String query(@RequestParam(value = "latitude", required = false) String latitude,
            @RequestParam(value = "longitude", required = false) String longitude,
            HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr() : forwarded.split(",", 2)[0].trim();
        return service.query(latitude, longitude, ip);
    }
}
