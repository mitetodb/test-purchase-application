package app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.list-export")
@Getter
@Setter
public class AppListExportProperties {

    /** Maximum rows allowed in a single CSV/Excel export (issue #15). */
    private int maxRows = 10_000;
}
