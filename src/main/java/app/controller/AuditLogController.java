package app.controller;

import app.model.dto.AuditLogRowDTO;
import app.model.enums.AuditAction;
import app.model.enums.AuditEntityType;
import app.repository.UserRepository;
import app.service.AuditLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/audit")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditLogController {

    private static final int PAGE_SIZE = 25;

    private final AuditLogQueryService auditLogQueryService;
    private final UserRepository userRepository;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(required = false) AuditAction action,
                       @RequestParam(required = false) AuditEntityType entityType,
                       @RequestParam(required = false) UUID actorUserId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                       Model model) {
        Page<AuditLogRowDTO> rows = auditLogQueryService.search(
                action,
                entityType,
                actorUserId,
                fromDate,
                toDate,
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "occurredAt")));

        model.addAttribute("auditRows", rows.getContent());
        model.addAttribute("page", rows.getNumber());
        model.addAttribute("totalPages", rows.getTotalPages());
        model.addAttribute("totalElements", rows.getTotalElements());
        model.addAttribute("filterAction", action);
        model.addAttribute("filterEntityType", entityType);
        model.addAttribute("filterActorUserId", actorUserId);
        model.addAttribute("filterFromDate", fromDate);
        model.addAttribute("filterToDate", toDate);
        model.addAttribute("allActions", AuditAction.values());
        model.addAttribute("allEntityTypes", AuditEntityType.values());
        model.addAttribute("allUsers", userRepository.findAll());
        return "audit/audit-list";
    }
}
