package app.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LoginRateLimitUpsert {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    private volatile String upsertSql;

    public void upsertIncrement(UUID id, String rateKey, LocalDateTime now, LocalDateTime windowNotBefore) {
        jdbcTemplate.update(
                resolveUpsertSql(),
                ps -> {
                    ps.setObject(1, id);
                    ps.setString(2, rateKey);
                    ps.setTimestamp(3, Timestamp.valueOf(now));
                    ps.setTimestamp(4, Timestamp.valueOf(windowNotBefore));
                    ps.setTimestamp(5, Timestamp.valueOf(windowNotBefore));
                    ps.setTimestamp(6, Timestamp.valueOf(now));
                });
    }

    private String resolveUpsertSql() {
        if (upsertSql != null) {
            return upsertSql;
        }
        synchronized (this) {
            if (upsertSql != null) {
                return upsertSql;
            }
            upsertSql = buildUpsertSql(isH2());
            return upsertSql;
        }
    }

    private boolean isH2() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName().contains("H2");
        } catch (SQLException e) {
            throw new IllegalStateException("Could not detect database product", e);
        }
    }

    private static String buildUpsertSql(boolean h2QuotedIdentifiers) {
        if (h2QuotedIdentifiers) {
            return """
                    INSERT INTO "login_rate_limit" ("id", "rate_key", "attempt_count", "window_start")
                    VALUES (?, ?, 1, ?)
                    ON DUPLICATE KEY UPDATE
                      "attempt_count" = CASE WHEN "window_start" >= ? THEN "attempt_count" + 1 ELSE 1 END,
                      "window_start" = CASE WHEN "window_start" >= ? THEN "window_start" ELSE ? END
                    """;
        }
        return """
                INSERT INTO login_rate_limit (id, rate_key, attempt_count, window_start)
                VALUES (?, ?, 1, ?)
                ON DUPLICATE KEY UPDATE
                  attempt_count = IF(window_start >= ?, attempt_count + 1, 1),
                  window_start = IF(window_start >= ?, window_start, ?)
                """;
    }
}
