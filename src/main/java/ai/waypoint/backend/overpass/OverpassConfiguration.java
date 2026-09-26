package ai.waypoint.backend.overpass;

import ai.waypoint.backend.ratelimit.RateLimiter;
import java.net.http.HttpClient;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Creates the shared JDK HTTP transport used only by the Overpass slice. */
@Configuration(proxyBeanMethods = false)
public class OverpassConfiguration {

    /** Independent request quota for the Overpass endpoint. */
    @Bean
    public RateLimiter overpassRateLimiter(OverpassProperties properties) {
        return new RateLimiter(properties.getRateMax(), properties.getRateWindow(), Clock.systemUTC());
    }

    /** Connection setup is bounded as well as each complete mirror request. */
    @Bean
    public HttpClient overpassHttpClient(OverpassProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.getUpstreamTimeout())
                .build();
    }
}
