package ai.waypoint.backend.elevation;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** Fetches raw elevation JSON with a deadline covering headers and the complete body. */
@Component
public class ElevationClient {

    private final HttpClient httpClient;
    private final ElevationProperties properties;

    public ElevationClient(@Qualifier("elevationHttpClient") HttpClient httpClient,
            ElevationProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    /** Fetch validated, normalized coordinate lists, preserving the upstream body verbatim. */
    public String fetch(String latitude, String longitude) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getBaseUrl()
                        + "?latitude=" + latitude + "&longitude=" + longitude))
                .timeout(properties.getUpstreamTimeout())
                .header("User-Agent", properties.getUserAgent())
                .header("Accept", "application/json").GET().build();
        CompletableFuture<HttpResponse<String>> pending = null;
        try {
            pending = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            HttpResponse<String> response = pending.get(
                    properties.getUpstreamTimeout().toNanos(), TimeUnit.NANOSECONDS);
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
        } catch (InterruptedException ex) {
            if (pending != null) {
                pending.cancel(true);
            }
            Thread.currentThread().interrupt();
            throw new UpstreamUnavailableException("Elevation request interrupted");
        } catch (ExecutionException | TimeoutException | RuntimeException ex) {
            if (pending != null) {
                pending.cancel(true);
            }
        }
        throw new UpstreamUnavailableException("Elevation provider failed");
    }
}
