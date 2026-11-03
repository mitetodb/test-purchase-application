package app.model.entity;

import app.model.enums.AuditAction;
import app.model.enums.AuditEntityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    private UUID id;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 32)
    private AuditEntityType entityType;

    @Column(name = "entity_id", length = 64)
    private String entityId;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "change_summary", nullable = false, length = 2000)
    private String changeSummary;
}
