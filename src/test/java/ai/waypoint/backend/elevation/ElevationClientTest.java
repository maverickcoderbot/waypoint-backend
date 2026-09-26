package ai.waypoint.backend.elevation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ElevationClientTest {

    private final HttpClient http = mock(HttpClient.class);
    private final ElevationProperties properties = new ElevationProperties();
    private final ElevationClient client = new ElevationClient(http, properties);

    @Test
    void returnsBodyVerbatimWithConfiguredHeadersAndCoordinates() {
        String body = "{ \"elevation\": [123.0, 456] }\n";
        respond(200, body);
        assertThat(client.fetch("38.6,38.7", "-90.2,-90.3")).isEqualTo(body);
        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).sendAsync(request.capture(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertThat(request.getValue().uri().toString()).isEqualTo(properties.getBaseUrl()
                + "?latitude=38.6,38.7&longitude=-90.2,-90.3");
        assertThat(request.getValue().headers().firstValue("User-Agent")).contains(properties.getUserAgent());
        assertThat(request.getValue().timeout()).contains(properties.getUpstreamTimeout());
    }

    @Test
    void httpFailureThrows() {
        respond(429, "busy");
        assertThatThrownBy(() -> client.fetch("38.6", "-90.2")).isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void transportFailureThrows() {
        when(http.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(CompletableFuture.failedFuture(new java.io.IOException("offline")));
        assertThatThrownBy(() -> client.fetch("38.6", "-90.2")).isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void deadlineCancelsPendingResponse() {
        properties.setUpstreamTimeout(Duration.ofMillis(1));
        CompletableFuture<HttpResponse<String>> pending = new CompletableFuture<>();
        when(http.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(pending);
        assertThatThrownBy(() -> client.fetch("38.6", "-90.2")).isInstanceOf(UpstreamUnavailableException.class);
        assertThat(pending.isCancelled()).isTrue();
    }

    @Test
    void interruptionPreservesFlagAndCancels() {
        CompletableFuture<HttpResponse<String>> pending = new CompletableFuture<>();
        when(http.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(pending);
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> client.fetch("38.6", "-90.2")).isInstanceOf(UpstreamUnavailableException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(pending.isCancelled()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    private void respond(int status, String body) {
        when(http.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(CompletableFuture.completedFuture(response(status, body)));
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> response(int status, String body) {
        return mock(HttpResponse.class, invocation -> switch (invocation.getMethod().getName()) {
            case "statusCode" -> status;
            case "body" -> body;
            default -> org.mockito.Answers.RETURNS_DEFAULTS.answer(invocation);
        });
    }
}
