package app.repository;

import app.model.entity.InviteToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface InviteTokenRepository extends JpaRepository<InviteToken, UUID> {
    Optional<InviteToken> findByTokenHash(String tokenHash);

    /**
     * Atomically mark a token as used. Returns the number of rows updated (1 if successful, 0 if already used or expired).
     */
    @Modifying
    @Query("UPDATE InviteToken t SET t.usedAt = :usedAt WHERE t.tokenHash = :tokenHash AND t.usedAt IS NULL AND t.expiresAt > :now")
    int markTokenAsUsed(@Param("tokenHash") String tokenHash, @Param("usedAt") LocalDateTime usedAt, @Param("now") LocalDateTime now);
}
