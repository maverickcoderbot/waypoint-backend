package ai.waypoint.backend.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Thread-safe fixed-window limiter with per-instance, in-memory state.
 *
 * <p>A window starts on a client's first request. Rejected requests do not extend
 * it. Instances do not share quotas; a multi-instance deployment needs a shared
 * limiter. A clock seam lets tests advance time without sleeping.
 */
public class RateLimiter {

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int max;
    private final Duration window;
    private final Clock clock;

    public RateLimiter(int max, Duration window, Clock clock) {
        if (max < 1 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Rate maximum and window must be positive");
        }
        this.max = max;
        this.window = window;
        this.clock = clock;
    }

    /** Consume one request for the key, throwing when its current window is full. */
    public void check(String key) {
        Instant now = clock.instant();
        windows.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
        AtomicBoolean allowed = new AtomicBoolean();
        windows.compute(key, (ignored, current) -> {
            if (current == null || !now.isBefore(current.expiresAt())) {
                allowed.set(true);
                return new Window(now.plus(window), 1);
            }
            if (current.count() < max) {
                allowed.set(true);
                return new Window(current.expiresAt(), current.count() + 1);
            }
            return current;
        });
        if (!allowed.get()) {
            throw new RateLimitException("Rate limit exceeded");
        }
    }

    private record Window(Instant expiresAt, int count) {
    }
}
