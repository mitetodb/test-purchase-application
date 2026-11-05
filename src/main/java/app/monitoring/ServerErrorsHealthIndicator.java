package app.monitoring;

import app.config.MonitoringProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component("serverErrors")
@RequiredArgsConstructor
public class ServerErrorsHealthIndicator implements HealthIndicator {

    private final MonitoringProperties properties;
    private final HttpServerErrorTracker tracker;

    @Override
    public Health health() {
        Duration window = Duration.ofSeconds(properties.getServerErrors().getWindowSeconds());
        int threshold = properties.getServerErrors().getThreshold();
        int recentCount = tracker.countRecentServerErrors(window);
        Health.Builder builder = recentCount >= threshold ? Health.down() : Health.up();
        return builder
                .withDetail("recent5xxCount", recentCount)
                .withDetail("windowSeconds", window.toSeconds())
                .withDetail("threshold", threshold)
                .build();
    }
}
