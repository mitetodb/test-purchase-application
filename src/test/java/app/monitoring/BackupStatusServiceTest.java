package app.monitoring;

import app.config.MonitoringProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class BackupStatusServiceTest {

    @TempDir
    Path tempDir;

    private MonitoringProperties properties;
    private MutableClock clock;
    private BackupStatusService service;

    @BeforeEach
    void setUp() {
        properties = new MonitoringProperties();
        clock = new MutableClock(Instant.parse("2026-11-05T12:00:00Z"));
        service = new BackupStatusService(properties, clock);
    }

    @Test
    void monitoringAlertDisabledByDefault() {
        assertThat(new MonitoringProperties().getAlert().isEnabled()).isFalse();
        assertThat(new MonitoringProperties().isBackupCheckEnabled()).isFalse();
    }

    @Test
    void freshMarkerWithinMaxAgeIsHealthy() throws Exception {
        properties.setBackupCheckEnabled(true);
        Path marker = tempDir.resolve("backup-last-success.timestamp");
        Files.writeString(marker, "2026-11-05T10:00:00Z");
        properties.setBackupMarkerPath(marker.toString());

        BackupStatusService.BackupAssessment assessment = service.assessBackupFreshness();

        assertThat(assessment.isHealthy()).isTrue();
        assertThat(assessment.status()).isEqualTo(BackupStatusService.BackupAssessment.Status.FRESH);
    }

    @Test
    void staleMarkerFailsHealth() throws Exception {
        properties.setBackupCheckEnabled(true);
        properties.setBackupMaxAgeHours(26);
        Path marker = tempDir.resolve("backup-last-success.timestamp");
        Files.writeString(marker, "2026-11-03T10:00:00Z");
        properties.setBackupMarkerPath(marker.toString());

        BackupStatusService.BackupAssessment assessment = service.assessBackupFreshness();

        assertThat(assessment.isHealthy()).isFalse();
        assertThat(assessment.status()).isEqualTo(BackupStatusService.BackupAssessment.Status.STALE);
    }

    @Test
    void missingMarkerWhenCheckEnabledIsUnhealthy() {
        properties.setBackupCheckEnabled(true);
        properties.setBackupMarkerPath(tempDir.resolve("missing").toString());

        BackupStatusService.BackupAssessment assessment = service.assessBackupFreshness();

        assertThat(assessment.isHealthy()).isFalse();
        assertThat(assessment.status()).isEqualTo(BackupStatusService.BackupAssessment.Status.MISSING);
    }

    @Test
    void backupCheckDisabledSkipsValidation() {
        properties.setBackupCheckEnabled(false);

        assertThat(service.assessBackupFreshness().status())
                .isEqualTo(BackupStatusService.BackupAssessment.Status.SKIPPED);
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
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
