package app.monitoring;

import app.config.MonitoringProperties;
import app.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringAlertService {

    private final MonitoringProperties properties;
    private final BackupStatusService backupStatusService;
    private final HttpServerErrorTracker serverErrorTracker;
    private final RestClient.Builder restClientBuilder;
    private final EmailService emailService;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    private Boolean lastOverallHealthy;

    @Scheduled(cron = "${app.monitoring.alert.cron:0 */5 * * * ?}")
    public void evaluateAndMaybeNotify() {
        if (!properties.getAlert().isEnabled()) {
            log.trace("Monitoring alerts disabled (app.monitoring.alert.enabled=false)");
            return;
        }

        boolean healthy = isCurrentlyHealthy();
        if (lastOverallHealthy != null && lastOverallHealthy == healthy) {
            return;
        }
        lastOverallHealthy = healthy;

        if (healthy) {
            log.info("Monitoring recovered; no alert sent");
            return;
        }

        List<String> issues = collectIssues();
        dispatchAlerts(issues);
    }

    boolean isCurrentlyHealthy() {
        return backupStatusService.assessBackupFreshness().isHealthy()
                && isServerErrorRateHealthy();
    }

    private boolean isServerErrorRateHealthy() {
        Duration window = Duration.ofSeconds(properties.getServerErrors().getWindowSeconds());
        int threshold = properties.getServerErrors().getThreshold();
        return serverErrorTracker.countRecentServerErrors(window) < threshold;
    }

    private List<String> collectIssues() {
        List<String> issues = new ArrayList<>();
        BackupStatusService.BackupAssessment backup = backupStatusService.assessBackupFreshness();
        if (!backup.isHealthy()) {
            issues.add("backup freshness check failed (" + backup.status() + ")");
        }
        if (!isServerErrorRateHealthy()) {
            issues.add("elevated application 5xx rate");
        }
        return issues;
    }

    private void dispatchAlerts(List<String> issues) {
        String summary = String.join("; ", issues);
        log.warn("Monitoring alert (demo stub): {}", summary);

        MonitoringProperties.Alert alert = properties.getAlert();
        if (StringUtils.hasText(alert.getWebhookUrl())) {
            try {
                restClientBuilder.build()
                        .post()
                        .uri(alert.getWebhookUrl())
                        .body(Map.of(
                                "source", "testkauf",
                                "summary", summary,
                                "issues", issues))
                        .retrieve()
                        .toBodilessEntity();
                log.info("Monitoring webhook delivered to configured URL");
            } catch (RuntimeException ex) {
                log.warn("Monitoring webhook call failed: {}", ex.getMessage());
            }
        }

        if (StringUtils.hasText(alert.getEmailTo())) {
            if (!mailEnabled) {
                log.info(
                        "Monitoring alert email skipped (app.mail.enabled=false); recipient={}",
                        alert.getEmailTo());
                return;
            }
            emailService.sendMonitoringAlertEmail(alert.getEmailTo(), summary, issues);
        }
    }
}
