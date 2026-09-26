package ai.waypoint.backend.overpass;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Environment-overridable proxy settings, bound under {@code waypoint.overpass}.
 *
 * <p>Defaults restrict requests to the St. Louis metro, cache for 24 hours, and
 * allow 30 requests per client in a 60-second window. Mirror order is significant.
 */
@Component
@ConfigurationProperties(prefix = "waypoint.overpass")
public class OverpassProperties {

    private GeoFence.Bbox bbox = new GeoFence.Bbox(38.35, -90.9, 39.05, -89.95);
    private Duration ttl = Duration.ofHours(24);
    private int rateMax = 30;
    private Duration rateWindow = Duration.ofSeconds(60);
    private List<URI> mirrors = List.of(
            URI.create("https://overpass-api.de/api/interpreter"),
            URI.create("https://maps.mail.ru/osm/tools/overpass/api/interpreter"),
            URI.create("https://overpass.private.coffee/api/interpreter"),
            URI.create("https://overpass.kumi.systems/api/interpreter"));
    private String userAgent = "Waypoint/1.0 (Overpass caching proxy)";
    private Duration upstreamTimeout = Duration.ofSeconds(30);

    public GeoFence.Bbox getBbox() {
        return bbox;
    }

    public void setBbox(GeoFence.Bbox bbox) {
        this.bbox = bbox;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public int getRateMax() {
        return rateMax;
    }

    public void setRateMax(int rateMax) {
        this.rateMax = rateMax;
    }

    public Duration getRateWindow() {
        return rateWindow;
    }

    public void setRateWindow(Duration rateWindow) {
        this.rateWindow = rateWindow;
    }

    public List<URI> getMirrors() {
        return mirrors;
    }

    public void setMirrors(List<URI> mirrors) {
        this.mirrors = mirrors;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Duration getUpstreamTimeout() {
        return upstreamTimeout;
    }

    public void setUpstreamTimeout(Duration upstreamTimeout) {
        this.upstreamTimeout = upstreamTimeout;
    }
}
