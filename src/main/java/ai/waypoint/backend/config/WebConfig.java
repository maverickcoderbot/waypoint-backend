package ai.waypoint.backend.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS policy for the browser PWA.
 *
 * <p>Allowed origins come from the {@code WAYPOINT_ALLOWED_ORIGINS} env var (comma
 * separated) so prod (GitHub Pages) and local dev can differ without code changes.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public WebConfig(
            @Value("${waypoint.allowed-origins:http://localhost:8000,http://127.0.0.1:8000}")
            List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST")
                .maxAge(3600);
    }
}
