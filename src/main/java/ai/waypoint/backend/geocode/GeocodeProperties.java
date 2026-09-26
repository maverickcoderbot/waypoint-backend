package ai.waypoint.backend.geocode;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Environment-overridable settings bound under {@code waypoint.geocode}. */
@Component
@ConfigurationProperties(prefix = "waypoint.geocode")
public class GeocodeProperties {

    private Duration ttl = Duration.ofDays(7);

    private int rateMax = 30;

    private Duration rateWindow = Duration.ofSeconds(60);

    private String userAgent = "Waypoint/1.0 (Geocode caching proxy)";

    private Duration upstreamTimeout = Duration.ofSeconds(30);

    private URI nominatimBaseUrl = URI.create("https://nominatim.openstreetmap.org/search");

    private URI openMeteoBaseUrl = URI.create("https://geocoding-api.open-meteo.com/v1/search");

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

    public URI getNominatimBaseUrl() {
        return nominatimBaseUrl;
    }

    public void setNominatimBaseUrl(URI nominatimBaseUrl) {
        this.nominatimBaseUrl = nominatimBaseUrl;
    }

    public URI getOpenMeteoBaseUrl() {
        return openMeteoBaseUrl;
    }

    public void setOpenMeteoBaseUrl(URI openMeteoBaseUrl) {
        this.openMeteoBaseUrl = openMeteoBaseUrl;
    }
}
