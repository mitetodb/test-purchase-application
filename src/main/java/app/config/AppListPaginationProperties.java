package app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.list-pagination")
@Getter
@Setter
public class AppListPaginationProperties {

    private int defaultPageSize = 20;
    private int maxPageSize = 100;
}
