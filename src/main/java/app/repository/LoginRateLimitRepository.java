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
    @Query("DELETE FROM LoginRateLimit r WHERE r.windowStart < :cutoff")
    int deleteExpiredWindows(@Param("cutoff") LocalDateTime cutoff);
}
