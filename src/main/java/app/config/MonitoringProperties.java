package app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.monitoring")
@Getter
@Setter
public class MonitoringProperties {

    /**
     * When true, backup freshness is required for the {@code backup} health component.
     */
    private boolean backupCheckEnabled = false;

    /**
     * File containing a single ISO-8601 instant (UTC) written by backup cron on the host.
     */
    private String backupMarkerPath = "";

    /**
     * Optional directory scanned for the newest {@code *.sql.gz} when no marker file is set.
     */
    private String backupScanDirectory = "";

    private int backupMaxAgeHours = 26;

    private ServerErrors serverErrors = new ServerErrors();

    private Alert alert = new Alert();

    @Getter
    @Setter
    public static class ServerErrors {
        private int windowSeconds = 300;
        private int threshold = 20;
    }

    @Getter
    @Setter
    public static class Alert {
        private boolean enabled = false;
        private String webhookUrl = "";
        private String emailTo = "";
        private String cron = "0 */5 * * * ?";
    }
}
