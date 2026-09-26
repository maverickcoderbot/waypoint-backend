package ai.waypoint.backend.elevation;

import ai.waypoint.backend.ratelimit.RateLimiter;
import java.net.http.HttpClient;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Independent transport and quota for the elevation endpoint. */
@Configuration(proxyBeanMethods = false)
public class ElevationConfiguration {

    /** Bound connection establishment as well as complete requests. */
    @Bean
    public HttpClient elevationHttpClient(ElevationProperties properties) {
        return HttpClient.newBuilder().connectTimeout(properties.getUpstreamTimeout()).build();
    }

    /** Keep quotas separate from other proxy slices. */
    @Bean
    public RateLimiter elevationRateLimiter(ElevationProperties properties) {
        return new RateLimiter(properties.getRateMax(), properties.getRateWindow(), Clock.systemUTC());
    }
}
