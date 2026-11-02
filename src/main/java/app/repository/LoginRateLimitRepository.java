package app.repository;

import app.model.entity.LoginRateLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface LoginRateLimitRepository extends JpaRepository<LoginRateLimit, UUID> {

    Optional<LoginRateLimit> findByRateKey(String rateKey);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE LoginRateLimit r
            SET r.attemptCount = r.attemptCount + 1
            WHERE r.rateKey = :rateKey
              AND r.windowStart >= :windowNotBefore
            """)
    int incrementAttemptInActiveWindow(
            @Param("rateKey") String rateKey,
            @Param("windowNotBefore") LocalDateTime windowNotBefore);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE LoginRateLimit r
            SET r.attemptCount = :count, r.windowStart = :now
            WHERE r.rateKey = :rateKey
              AND r.windowStart < :windowNotBefore
            """)
    int resetWindowAndSetAttempts(
            @Param("rateKey") String rateKey,
            @Param("windowNotBefore") LocalDateTime windowNotBefore,
            @Param("now") LocalDateTime now,
            @Param("count") int count);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM LoginRateLimit r WHERE r.windowStart < :cutoff")
    int deleteExpiredWindows(@Param("cutoff") LocalDateTime cutoff);
}
