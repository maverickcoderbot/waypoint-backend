package ai.waypoint.backend.overpass;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Browser-facing Overpass proxy accepting raw QL and returning raw JSON. */
@RestController
@RequestMapping("/api/overpass")
@Validated
public class OverpassController {

    private final OverpassService service;

    public OverpassController(OverpassService service) {
        this.service = service;
    }

    /**
     * Resolve the first forwarded hop, falling back to the servlet remote address.
     * The ingress proxy must sanitize X-Forwarded-For for quotas to identify clients.
     * Empty bodies reach the service so they consume quota and fail the geofence.
     */
    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String query(@RequestBody(required = false) String query, HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded == null || forwarded.isBlank()
                ? request.getRemoteAddr() : forwarded.split(",", 2)[0].trim();
        return service.query(query == null ? "" : query, ip);
    }
}
