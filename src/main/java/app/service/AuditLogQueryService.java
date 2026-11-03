package app.service;

import app.model.dto.AuditLogRowDTO;
import app.model.entity.AuditLog;
import app.model.enums.AuditAction;
import app.model.enums.AuditEntityType;
import app.repository.AuditLogRepository;
import app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<AuditLogRowDTO> search(AuditAction action,
                                       AuditEntityType entityType,
                                       UUID actorUserId,
                                       LocalDate fromDate,
                                       LocalDate toDate,
                                       Pageable pageable) {
        Specification<AuditLog> spec = Specification.where(null);
        if (action != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("action"), action));
        }
        if (entityType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityType"), entityType));
        }
        if (actorUserId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("actorUserId"), actorUserId));
        }
        if (fromDate != null) {
            LocalDateTime from = fromDate.atStartOfDay();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
        }
        if (toDate != null) {
            LocalDateTime to = toDate.atTime(LocalTime.MAX);
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("occurredAt"), to));
        }

        Page<AuditLog> page = auditLogRepository.findAll(spec, pageable);
        Set<UUID> actorIds = page.getContent().stream()
                .map(AuditLog::getActorUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<UUID, String> usernames = actorIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(actorIds).stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u.getUsername()));

        return page.map(row -> AuditLogRowDTO.builder()
                .id(row.getId())
                .occurredAt(row.getOccurredAt())
                .actorUserId(row.getActorUserId())
                .actorUsername(row.getActorUserId() == null ? null : usernames.get(row.getActorUserId()))
                .action(row.getAction())
                .entityType(row.getEntityType())
                .entityId(row.getEntityId())
                .clientIp(row.getClientIp())
                .changeSummary(row.getChangeSummary())
                .build());
    }
}
