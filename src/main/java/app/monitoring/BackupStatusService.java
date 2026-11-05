package app.monitoring;

import app.config.MonitoringProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class BackupStatusService {

    private final MonitoringProperties properties;
    private final Clock clock;

    public boolean isBackupCheckEnabled() {
        return properties.isBackupCheckEnabled();
    }

    public Optional<Instant> resolveLastSuccessInstant() {
        if (StringUtils.hasText(properties.getBackupMarkerPath())) {
            return readMarkerInstant(Path.of(properties.getBackupMarkerPath()));
        }
        if (StringUtils.hasText(properties.getBackupScanDirectory())) {
            return newestSqlGzipModifiedInstant(Path.of(properties.getBackupScanDirectory()));
        }
        return Optional.empty();
    }

    public BackupAssessment assessBackupFreshness() {
        if (!properties.isBackupCheckEnabled()) {
            return BackupAssessment.skipped();
        }
        Optional<Instant> lastSuccess = resolveLastSuccessInstant();
        if (lastSuccess.isEmpty()) {
            return BackupAssessment.missing();
        }
        Duration age = Duration.between(lastSuccess.get(), clock.instant());
        long maxAgeHours = properties.getBackupMaxAgeHours();
        if (age.compareTo(Duration.ofHours(maxAgeHours)) >= 0) {
            return BackupAssessment.stale(lastSuccess.get(), age, maxAgeHours);
        }
        return BackupAssessment.fresh(lastSuccess.get(), age, maxAgeHours);
    }

    private Optional<Instant> readMarkerInstant(Path markerFile) {
        if (!Files.isRegularFile(markerFile)) {
            return Optional.empty();
        }
        try {
            String raw = Files.readString(markerFile).trim();
            if (!StringUtils.hasText(raw)) {
                return Optional.empty();
            }
            return Optional.of(Instant.parse(raw));
        } catch (IOException | DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    private Optional<Instant> newestSqlGzipModifiedInstant(Path directory) {
        if (!Files.isDirectory(directory)) {
            return Optional.empty();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".sql.gz"))
                    .max(Comparator.comparing(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toInstant();
                        } catch (IOException ex) {
                            return Instant.EPOCH;
                        }
                    }))
                    .map(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toInstant();
                        } catch (IOException ex) {
                            return null;
                        }
                    })
                    .filter(instant -> instant != null);
        } catch (IOException ex) {
            return Optional.empty();
        }
    }

    public record BackupAssessment(
            Status status,
            Instant lastSuccess,
            Duration age,
            long maxAgeHours) {

        enum Status {
            SKIPPED,
            MISSING,
            STALE,
            FRESH
        }

        static BackupAssessment skipped() {
            return new BackupAssessment(Status.SKIPPED, null, null, 0);
        }

        static BackupAssessment missing() {
            return new BackupAssessment(Status.MISSING, null, null, 0);
        }

        static BackupAssessment stale(Instant lastSuccess, Duration age, long maxAgeHours) {
            return new BackupAssessment(Status.STALE, lastSuccess, age, maxAgeHours);
        }

        static BackupAssessment fresh(Instant lastSuccess, Duration age, long maxAgeHours) {
            return new BackupAssessment(Status.FRESH, lastSuccess, age, maxAgeHours);
        }

        boolean isHealthy() {
            return status == Status.SKIPPED || status == Status.FRESH;
        }
    }
}
