package ai.waypoint.backend.geocode;

import ai.waypoint.backend.ratelimit.RateLimiter;
import java.net.http.HttpClient;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Independent transport and quota for the geocode endpoint. */
@Configuration(proxyBeanMethods = false)
public class GeocodeConfiguration {

    /** Bound connection establishment as well as complete requests. */
    @Bean
    public HttpClient geocodeHttpClient(GeocodeProperties properties) {
        return HttpClient.newBuilder().connectTimeout(properties.getUpstreamTimeout()).build();
    }

    /** Keep quotas separate from other proxy slices. */
    @Bean
    public RateLimiter geocodeRateLimiter(GeocodeProperties properties) {
        return new RateLimiter(properties.getRateMax(), properties.getRateWindow(), Clock.systemUTC());
    }
}
