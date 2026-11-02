package app.repository;

import app.model.entity.UserTotpRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserTotpRecoveryCodeRepository extends JpaRepository<UserTotpRecoveryCode, UUID> {

    @Query("SELECT c FROM UserTotpRecoveryCode c WHERE c.user.id = :userId AND c.usedAt IS NULL")
    List<UserTotpRecoveryCode> findUnusedByUserId(@Param("userId") UUID userId);

    void deleteByUser_Id(UUID userId);
}
