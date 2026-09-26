package ai.waypoint.backend.overpass;

import java.net.URI;
import java.net.URLEncoder;
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

/**
 * Posts raw Overpass QL to mirrors in configured order, returning the first 2xx body.
 *
 * <p>The injected transport supports offline tests. Each attempt has a deadline
 * covering headers and the complete body; timed-out futures are cancelled before
 * trying the next mirror. Interrupts abort fallback and preserve the interrupt flag.
 */
@Component
public class OverpassClient {

    private final HttpClient httpClient;
    private final OverpassProperties properties;

    public OverpassClient(@Qualifier("overpassHttpClient") HttpClient httpClient,
            OverpassProperties properties) {
        this.httpClient = httpClient;
        this.properties = properties;
    }

    /** Fetch a response, or throw when every mirror fails or the caller is interrupted. */
    public String fetch(String query) {
        for (URI mirror : properties.getMirrors()) {
            HttpRequest request = HttpRequest.newBuilder(mirror)
                    .timeout(properties.getUpstreamTimeout())
                    .header("User-Agent", properties.getUserAgent())
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8),
                            StandardCharsets.UTF_8))
                    .build();
            CompletableFuture<HttpResponse<String>> pending = httpClient.sendAsync(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            try {
                HttpResponse<String> response = pending.get(
                        properties.getUpstreamTimeout().toNanos(), TimeUnit.NANOSECONDS);
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return response.body();
                }
            } catch (ExecutionException | TimeoutException ex) {
                pending.cancel(true);
                // Transport errors and timeouts advance to the next configured mirror.
            } catch (InterruptedException ex) {
                pending.cancel(true);
                Thread.currentThread().interrupt();
                throw new UpstreamUnavailableException("Overpass request interrupted");
            }
        }
        throw new UpstreamUnavailableException("All Overpass mirrors failed");
    }
}
