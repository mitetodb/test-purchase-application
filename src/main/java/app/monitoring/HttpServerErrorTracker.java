package app.monitoring;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedDeque;

public class HttpServerErrorTracker {

    private final Clock clock;
    private final ConcurrentLinkedDeque<Instant> serverErrors = new ConcurrentLinkedDeque<>();

    public HttpServerErrorTracker(Clock clock) {
        this.clock = clock;
    }

    public void recordServerError() {
        Instant now = clock.instant();
        serverErrors.addLast(now);
        pruneOlderThan(now.minus(Duration.ofHours(1)));
    }

    public int countRecentServerErrors(Duration window) {
        Instant now = clock.instant();
        Instant cutoff = now.minus(window);
        pruneOlderThan(cutoff);
        int count = 0;
        for (Instant event : serverErrors) {
            if (!event.isBefore(cutoff)) {
                count++;
            }
        }
        return count;
    }

    private void pruneOlderThan(Instant cutoff) {
        Instant first;
        while ((first = serverErrors.peekFirst()) != null && first.isBefore(cutoff)) {
            serverErrors.pollFirst();
        }
    }

    void clearForTests() {
        serverErrors.clear();
    }
}
