package app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.deadline-reminder")
@Getter
@Setter
public class DeadlineReminderProperties {

    private boolean enabled = false;

    /**
     * Days before the deadline when a reminder is sent (0 = on the deadline date).
     */
    private List<Integer> daysBefore = new ArrayList<>(List.of(7, 1, 0));

    private String cron = "0 0 8 * * ?";
}
