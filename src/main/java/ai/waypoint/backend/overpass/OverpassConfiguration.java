package ai.waypoint.backend.overpass;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Creates the shared JDK HTTP transport used only by the Overpass slice. */
@Configuration(proxyBeanMethods = false)
public class OverpassConfiguration {

    /** Connection setup is bounded as well as each complete mirror request. */
    @Bean
    public HttpClient overpassHttpClient(OverpassProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(properties.getUpstreamTimeout())
                .build();
    }
}
