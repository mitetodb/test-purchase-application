package app.db;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationV12Test {

    @Test
    void v12AddsDeadlineColumnAndReminderTable() throws Exception {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("db/migration/V12__test_purchase_deadlines_and_reminders.sql")) {
            assertThat(in).isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(sql).containsIgnoringCase("deadline_at");
            assertThat(sql).containsIgnoringCase("test_purchase_deadline_reminders");
            assertThat(sql).containsIgnoringCase("days_before_offset");
            assertThat(sql).containsIgnoringCase("uk_tp_deadline_reminder");
        }
    }
}
