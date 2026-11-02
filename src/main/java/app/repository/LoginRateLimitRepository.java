package app.repository;

import app.model.entity.LoginRateLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LoginRateLimitRepository extends JpaRepository<LoginRateLimit, UUID> {

    Optional<LoginRateLimit> findByRateKey(String rateKey);
}
