package app.monitoring;

import app.config.MonitoringProperties;
import app.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MonitoringAlertServiceTest {

    @Mock
    private BackupStatusService backupStatusService;

    @Mock
    private HttpServerErrorTracker serverErrorTracker;

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private EmailService emailService;

    private MonitoringProperties properties;
    private MonitoringAlertService alertService;

    @BeforeEach
    void setUp() {
        properties = new MonitoringProperties();
        alertService = new MonitoringAlertService(
                properties,
                backupStatusService,
                serverErrorTracker,
                restClientBuilder,
                emailService);
    }

    @Test
    void evaluateAndMaybeNotifyIsNoOpWhenAlertsDisabled() {
        alertService.evaluateAndMaybeNotify();

        verifyNoInteractions(restClientBuilder, emailService, backupStatusService, serverErrorTracker);
    }

    @Test
    void evaluateAndMaybeNotifyDoesNotSendWhenHealthyAndEnabled() {
        properties.getAlert().setEnabled(true);
        org.mockito.Mockito.when(backupStatusService.assessBackupFreshness())
                .thenReturn(BackupStatusService.BackupAssessment.skipped());
        org.mockito.Mockito.when(serverErrorTracker.countRecentServerErrors(Duration.ofSeconds(300)))
                .thenReturn(0);

        alertService.evaluateAndMaybeNotify();

        verify(restClientBuilder, never()).build();
        verify(emailService, never()).sendMonitoringAlertEmail(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void isCurrentlyHealthyWhenBackupSkippedAndLow5xxRate() {
        org.mockito.Mockito.when(backupStatusService.assessBackupFreshness())
                .thenReturn(BackupStatusService.BackupAssessment.skipped());
        org.mockito.Mockito.when(serverErrorTracker.countRecentServerErrors(Duration.ofSeconds(300)))
                .thenReturn(1);

        assertThat(alertService.isCurrentlyHealthy()).isTrue();
    }
}
