package app.service;

import app.audit.AuditChangeSummary;
import app.audit.AuditContext;
import app.audit.AuditSummarySanitizer;
import app.config.SecurityUtils;
import app.model.entity.AuditLog;
import app.model.enums.AuditAction;
import app.model.enums.AuditEntityType;
import app.repository.AuditLogRepository;
import app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void record(AuditAction action,
                       AuditEntityType entityType,
                       UUID entityId,
                       String changeSummary) {
        record(action, entityType, entityId, changeSummary, null, null);
    }

    @Transactional
    public void record(AuditAction action,
                       AuditEntityType entityType,
                       UUID entityId,
                       String changeSummary,
                       UUID actorUserIdOverride) {
        record(action, entityType, entityId, changeSummary, actorUserIdOverride, null);
    }

    @Transactional
    public void record(AuditAction action,
                       AuditEntityType entityType,
                       UUID entityId,
                       String changeSummary,
                       UUID actorUserIdOverride,
                       String clientIpOverride) {
        UUID actorId = actorUserIdOverride;
        if (actorId == null) {
            actorId = resolveCurrentActorUserId();
        }
        String clientIp = clientIpOverride != null ? clientIpOverride : AuditContext.getClientIp();
        String summary = AuditSummarySanitizer.sanitize(changeSummary);

        AuditLog entry = AuditLog.builder()
                .id(UUID.randomUUID())
                .occurredAt(LocalDateTime.now())
                .actorUserId(actorId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId != null ? entityId.toString() : null)
                .clientIp(clientIp)
                .changeSummary(summary)
                .build();
        auditLogRepository.save(entry);
    }

    @Transactional
    public void recordAuthEvent(AuditAction action,
                                UUID subjectUserId,
                                String username,
                                String clientIp) {
        String summary = AuditChangeSummary.kv("username", username);
        UUID actorOverride = action == AuditAction.LOGIN_SUCCESS ? subjectUserId : null;
        record(action, AuditEntityType.AUTH, subjectUserId, summary, actorOverride, clientIp);
    }

    private UUID resolveCurrentActorUserId() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null || "ANONYMOUS".equals(username)) {
            return null;
        }
        return userRepository.findByUsername(username).map(u -> u.getId()).orElse(null);
    }
}
