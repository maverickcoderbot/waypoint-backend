package ai.waypoint.backend.geocode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class GeocodeClientTest {

    private final HttpClient http = mock(HttpClient.class);
    private final GeocodeProperties properties = new GeocodeProperties();
    private final GeocodeClient client = new GeocodeClient(http, properties, JsonMapper.builder().build());

    @Test
    void nominatimSuccessIncludesComplianceHeadersAndEncodedQuery() {
        respond(200, "[{\"display_name\":\"St. Louis\",\"lat\":\"38.6\",\"lon\":\"-90.2\"}]");
        assertThat(client.fetch("St. Louis & park"))
                .containsExactly(new GeocodeResult("St. Louis", 38.6, -90.2));
        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).sendAsync(request.capture(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertThat(request.getValue().uri().toString()).isEqualTo(properties.getNominatimBaseUrl()
                + "?format=jsonv2&addressdetails=0&limit=5&q=St.+Louis+%26+park");
        assertThat(request.getValue().headers().firstValue("User-Agent")).contains(properties.getUserAgent());
        assertThat(request.getValue().timeout()).contains(properties.getUpstreamTimeout());
        assertThat(request.getValue().method()).isEqualTo("GET");
    }

    @Test
    void failedNominatimFallsBackToOpenMeteo() {
        when(http.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(CompletableFuture.completedFuture(response(503, "")),
                        CompletableFuture.completedFuture(response(200,
                                "{\"results\":[{\"name\":\"Park\",\"latitude\":38.6,\"longitude\":-90.2}]}")));
        assertThat(client.fetch("Park")).containsExactly(new GeocodeResult("Park", 38.6, -90.2));
        ArgumentCaptor<HttpRequest> requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http, times(2)).sendAsync(requests.capture(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        assertThat(requests.getAllValues().get(1).uri().toString()).isEqualTo(properties.getOpenMeteoBaseUrl()
                + "?count=5&language=en&name=Park");
    }

    @Test
    void bothEmptyReturnEmptyList() {
        when(http.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(CompletableFuture.completedFuture(response(200, "[]")),
                        CompletableFuture.completedFuture(response(200, "{}")));
        assertThat(client.fetch("unknown")).isEmpty();
        verify(http, times(2)).sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
    }

    @Test
    void malformedNominatimFallsBack() {
        when(http.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(CompletableFuture.completedFuture(response(200, "not json")),
                        CompletableFuture.completedFuture(response(200, "{\"results\":[]}")));
        assertThat(client.fetch("unknown")).isEmpty();
    }

    @Test
    void bothTransportFailuresThrow() {
        when(http.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(CompletableFuture.failedFuture(new java.io.IOException("offline")));
        assertThatThrownBy(() -> client.fetch("park")).isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void deadlineCancelsAndFallsBack() {
        properties.setUpstreamTimeout(Duration.ofMillis(1));
        CompletableFuture<HttpResponse<String>> pending = new CompletableFuture<>();
        when(http.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(pending, CompletableFuture.completedFuture(response(200, "{}")));
        assertThat(client.fetch("park")).isEmpty();
        assertThat(pending.isCancelled()).isTrue();
    }

    @Test
    void interruptionCancelsAndAbortsFallback() {
        CompletableFuture<HttpResponse<String>> pending = new CompletableFuture<>();
        when(http.sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenReturn(pending);
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> client.fetch("park")).isInstanceOf(UpstreamUnavailableException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
            assertThat(pending.isCancelled()).isTrue();
            verify(http).sendAsync(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
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
