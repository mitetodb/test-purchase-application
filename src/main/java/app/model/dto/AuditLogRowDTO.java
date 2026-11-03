package app.model.dto;

import app.model.enums.AuditAction;
import app.model.enums.AuditEntityType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AuditLogRowDTO {
    private UUID id;
    private LocalDateTime occurredAt;
    private UUID actorUserId;
    private String actorUsername;
    private AuditAction action;
    private AuditEntityType entityType;
    private String entityId;
    private String clientIp;
    private String changeSummary;
}
