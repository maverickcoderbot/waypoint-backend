package ai.waypoint.backend.geocode;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Tries Nominatim first, falling back on failures or empty results to Open-Meteo.
 * Complete response deadlines bound both attempts; interrupts abort fallback.
 */
@Component
public class GeocodeClient {

    private final HttpClient httpClient;
    private final GeocodeProperties properties;
    private final ObjectMapper mapper;

    public GeocodeClient(@Qualifier("geocodeHttpClient") HttpClient httpClient,
            GeocodeProperties properties, ObjectMapper mapper) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.mapper = mapper;
    }

    /** Return normalized candidates, or empty when neither provider finds a match. */
    public List<GeocodeResult> fetch(String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        List<GeocodeResult> first = attempt(URI.create(properties.getNominatimBaseUrl()
                + "?format=jsonv2&addressdetails=0&limit=5&q=" + encoded), true);
        if (first != null && !first.isEmpty()) {
            return first;
        }
        List<GeocodeResult> second = attempt(URI.create(properties.getOpenMeteoBaseUrl()
                + "?count=5&language=en&name=" + encoded), false);
        if (second != null) {
            return second;
        }
        if (first != null) {
            return first;
        }
        throw new UpstreamUnavailableException("All geocode providers failed");
    }

    private List<GeocodeResult> attempt(URI uri, boolean nominatim) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(properties.getUpstreamTimeout())
                .header("User-Agent", properties.getUserAgent())
                .header("Accept", "application/json").GET().build();
        CompletableFuture<HttpResponse<String>> pending = null;
        try {
            pending = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            HttpResponse<String> response = pending.get(
                    properties.getUpstreamTimeout().toNanos(), TimeUnit.NANOSECONDS);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return null;
            }
            JsonNode root = mapper.readTree(response.body());
            if (root == null || (nominatim ? !root.isArray() : !root.isObject())) {
                return null;
            }
            JsonNode candidates = nominatim ? root : root.path("results");
            if (!nominatim && candidates.isMissingNode()) {
                return List.of();
            }
            if (!candidates.isArray()) {
                return null;
            }
            List<GeocodeResult> results = new ArrayList<>();
            for (JsonNode candidate : candidates) {
                String name = candidate.path(nominatim ? "display_name" : "name").asString("");
                double lat = Double.parseDouble(candidate.path(nominatim ? "lat" : "latitude").asString());
                double lon = Double.parseDouble(candidate.path(nominatim ? "lon" : "longitude").asString());
                if (name.isBlank() || !Double.isFinite(lat) || !Double.isFinite(lon)
                        || Math.abs(lat) > 90 || Math.abs(lon) > 180) {
                    return null;
                }
                results.add(new GeocodeResult(name, lat, lon));
            }
            return results;
        } catch (InterruptedException ex) {
            if (pending != null) {
                pending.cancel(true);
            }
            Thread.currentThread().interrupt();
            throw new UpstreamUnavailableException("Geocode request interrupted");
        } catch (ExecutionException | TimeoutException | RuntimeException ex) {
            if (pending != null) {
                pending.cancel(true);
            }
            return null;
        }
    }
}
