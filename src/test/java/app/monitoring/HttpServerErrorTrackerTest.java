package app.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class HttpServerErrorTrackerTest {

    private MutableClock clock;
    private HttpServerErrorTracker tracker;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-05T12:00:00Z"));
        tracker = new HttpServerErrorTracker(clock);
    }

    @Test
    void countsErrorsInsideWindowOnly() {
        tracker.recordServerError();
        tracker.recordServerError();
        assertThat(tracker.countRecentServerErrors(Duration.ofMinutes(5))).isEqualTo(2);

        clock.advance(Duration.ofMinutes(6));
        assertThat(tracker.countRecentServerErrors(Duration.ofMinutes(5))).isZero();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
