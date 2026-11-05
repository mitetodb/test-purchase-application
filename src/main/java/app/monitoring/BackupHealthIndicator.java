package app.monitoring;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("backup")
@RequiredArgsConstructor
public class BackupHealthIndicator implements HealthIndicator {

    private final BackupStatusService backupStatusService;

    @Override
    public Health health() {
        BackupStatusService.BackupAssessment assessment = backupStatusService.assessBackupFreshness();
        return switch (assessment.status()) {
            case SKIPPED -> Health.up()
                    .withDetail("checkEnabled", false)
                    .withDetail("detail", "backup freshness check disabled")
                    .build();
            case MISSING -> Health.down()
                    .withDetail("checkEnabled", true)
                    .withDetail("reason", "missing or unreadable backup marker/dump")
                    .build();
            case STALE -> Health.down()
                    .withDetail("checkEnabled", true)
                    .withDetail("lastSuccess", assessment.lastSuccess().toString())
                    .withDetail("ageHours", assessment.age().toHours())
                    .withDetail("maxAgeHours", assessment.maxAgeHours())
                    .build();
            case FRESH -> Health.up()
                    .withDetail("checkEnabled", true)
                    .withDetail("lastSuccess", assessment.lastSuccess().toString())
                    .withDetail("ageHours", assessment.age().toHours())
                    .withDetail("maxAgeHours", assessment.maxAgeHours())
                    .build();
        };
    }
}
