package app.model.entity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_totp_recovery_code")
public class UserTotpRecoveryCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "invalidated_at")
    private LocalDateTime invalidatedAt;

    @Column(name = "invalidation_reason", length = 64)
    private String invalidationReason;

    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;

    @PrePersist
    void onCreate() {
        if (createdOn == null) {
            createdOn = LocalDateTime.now();
        }
    }
}
