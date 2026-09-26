package ai.waypoint.backend.overpass;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private final Clock clock = mock(Clock.class);
    private final Instant start = Instant.parse("2026-01-01T00:00:00Z");
    private final RateLimiter limiter = new RateLimiter(2, Duration.ofSeconds(60), clock);

    @Test
    void allowsMaximumAndBlocksNextRequest() {
        when(clock.instant()).thenReturn(start);
        limiter.check("client");
        limiter.check("client");
        assertThatThrownBy(() -> limiter.check("client")).isInstanceOf(RateLimitException.class);
        assertThatCode(() -> limiter.check("other-client")).doesNotThrowAnyException();
    }

    @Test
    void resetsAtExactWindowBoundaryWithoutExtendingWindowOnRejection() {
        when(clock.instant()).thenReturn(start);
        limiter.check("client");
        limiter.check("client");
        when(clock.instant()).thenReturn(start.plusSeconds(59));
        assertThatThrownBy(() -> limiter.check("client")).isInstanceOf(RateLimitException.class);
        when(clock.instant()).thenReturn(start.plusSeconds(60));
        limiter.check("client");
        limiter.check("client");
        assertThatThrownBy(() -> limiter.check("client")).isInstanceOf(RateLimitException.class);
    }
}
