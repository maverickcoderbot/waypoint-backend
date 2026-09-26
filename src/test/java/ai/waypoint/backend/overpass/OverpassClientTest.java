package ai.waypoint.backend.overpass;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OverpassClientTest {

    @Mock
    private HttpClient httpClient;

    private final OverpassProperties properties = new OverpassProperties();
    private OverpassClient client;

    @BeforeEach
    void setUp() {
        properties.setMirrors(List.of(URI.create("https://first.example/api/interpreter"),
                URI.create("https://second.example/api/interpreter")));
        client = new OverpassClient(httpClient, properties);
    }

    @Test
    void nonSuccessfulFirstMirrorFallsBackToSecond() {
        when(httpClient.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(CompletableFuture.completedFuture(response(503, null)),
                        CompletableFuture.completedFuture(response(200, "{\"elements\":[]}")));

        assertThat(client.fetch("node(around:100,38.6,-90.3);out;"))
                .isEqualTo("{\"elements\":[]}");

        ArgumentCaptor<HttpRequest> requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).sendAsync(requests.capture(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertThat(requests.getAllValues()).extracting(HttpRequest::uri)
                .containsExactlyElementsOf(properties.getMirrors());
        for (HttpRequest request : requests.getAllValues()) {
            assertThat(request.method()).isEqualTo("POST");
            assertThat(request.timeout()).contains(properties.getUpstreamTimeout());
            assertThat(request.headers().firstValue("User-Agent")).contains(properties.getUserAgent());
            assertThat(request.headers().firstValue("Content-Type")).contains("application/x-www-form-urlencoded");
        }
    }

    @Test
    void transportFailureFallsBackToSecondMirror() {
        when(httpClient.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(CompletableFuture.failedFuture(new IOException("offline")),
                        CompletableFuture.completedFuture(response(200, "{}")));
        assertThat(client.fetch("query")).isEqualTo("{}");
    }

    @Test
    void allMirrorsFail() {
        when(httpClient.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(CompletableFuture.failedFuture(new IOException("offline")),
                        CompletableFuture.completedFuture(response(429, null)));
        assertThatThrownBy(() -> client.fetch("query"))
                .isInstanceOf(UpstreamUnavailableException.class)
                .hasMessage("All Overpass mirrors failed");
        verify(httpClient, times(2)).sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    }

    @Test
    void deadlineCancelsPendingBodyAndFallsBack() {
        properties.setUpstreamTimeout(Duration.ofMillis(1));
        CompletableFuture<HttpResponse<String>> pending = new CompletableFuture<>();
        when(httpClient.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(pending, CompletableFuture.completedFuture(response(200, "{}")));
        assertThat(client.fetch("query")).isEqualTo("{}");
        assertThat(pending.isCancelled()).isTrue();
    }

    @Test
    void successSkipsRemainingMirrors() {
        when(httpClient.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(CompletableFuture.completedFuture(response(200, "{}")));
        assertThat(client.fetch("query")).isEqualTo("{}");
        verify(httpClient).sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
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
