package ai.waypoint.backend.elevation;

import ai.waypoint.backend.geo.Bbox;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Environment-overridable settings bound under {@code waypoint.elevation}. */
@Component
@ConfigurationProperties(prefix = "waypoint.elevation")
public class ElevationProperties {

    private Duration ttl = Duration.ofDays(30);

    private int rateMax = 30;

    private Duration rateWindow = Duration.ofSeconds(60);

    private String userAgent = "Waypoint/1.0 (Elevation caching proxy)";

    private Duration upstreamTimeout = Duration.ofSeconds(30);

    private int maxPoints = 512;

    private Bbox bbox = new Bbox(38.35, -90.9, 39.05, -89.95);

    private URI baseUrl = URI.create("https://api.open-meteo.com/v1/elevation");

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

    public int getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(int maxPoints) {
        this.maxPoints = maxPoints;
    }

    public Bbox getBbox() {
        return bbox;
    }

    public void setBbox(Bbox bbox) {
        this.bbox = bbox;
    }

    public URI getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = baseUrl;
    }
}
