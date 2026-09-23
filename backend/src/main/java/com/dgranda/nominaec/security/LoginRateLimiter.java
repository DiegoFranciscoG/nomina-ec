package com.dgranda.nominaec.security;

import com.dgranda.nominaec.config.AppProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window limiter for login attempts per client key (IP). In-memory is enough for a single
 * instance on the free tier; a multi-instance deploy would move this to Redis or the gateway.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_TRACKED_KEYS = 10_000;

    private final int maxAttempts;
    private final long windowMillis;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public LoginRateLimiter(AppProperties properties, Clock clock) {
        this.maxAttempts = properties.loginRateLimit().maxAttempts();
        this.windowMillis = properties.loginRateLimit().windowSeconds() * 1000L;
        this.clock = clock;
    }

    /** Registers an attempt and returns false when the key exceeded the limit in the current window. */
    public boolean tryAcquire(String key) {
        long now = clock.millis();
        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.entrySet().removeIf(e -> now - e.getValue().start >= windowMillis);
        }
        Window w = windows.compute(key, (k, current) ->
                current == null || now - current.start >= windowMillis ? new Window(now, 1) : new Window(current.start, current.count + 1));
        return w.count <= maxAttempts;
    }

    public void reset(String key) {
        windows.remove(key);
    }

    private record Window(long start, int count) {
    }
}
