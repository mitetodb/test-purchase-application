package app.service.impl;

import app.audit.AuditChangeSummary;
import app.config.SecurityUtils;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.AuditAction;
import app.model.enums.AuditEntityType;
import app.model.enums.Role;
import app.model.enums.TestPurchaseStatus;
import app.repository.TestPurchaseRepository;
import app.service.AccessControlService;
import app.service.AuditLogService;
import app.service.EmailService;
import app.service.VisitAppointmentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VisitAppointmentServiceImpl implements VisitAppointmentService {

    private final TestPurchaseRepository testPurchaseRepository;
    private final AccessControlService accessControlService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    @Override
    @Transactional
    public void scheduleOrUpdate(UUID testPurchaseId,
                                 LocalDateTime scheduledAt,
                                 String location,
                                 String notes,
                                 boolean confirm) {
        if (scheduledAt == null) {
            throw new IllegalArgumentException("Visit date and time are required");
        }
        if (scheduledAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Visit appointment must be in the future");
        }

        TestPurchase purchase = loadForAppointmentChange(testPurchaseId);
        accessControlService.checkCanManageVisitAppointment(purchase);

        User currentUser = accessControlService.getCurrentUser();
        boolean confirmed = resolveConfirmedOnSave(currentUser, confirm);

        LocalDateTime previousAt = purchase.getVisitAppointmentAt();
        boolean wasConfirmed = purchase.isVisitAppointmentConfirmed();

        purchase.setVisitAppointmentAt(scheduledAt);
        purchase.setVisitAppointmentLocation(trimToNull(location));
        purchase.setVisitAppointmentNotes(trimToNull(notes));
        purchase.setVisitAppointmentConfirmed(confirmed);
        purchase.setUpdatedByUser(SecurityUtils.getCurrentUsername());
        testPurchaseRepository.save(purchase);

        auditLogService.record(
                AuditAction.APPOINTMENT_SCHEDULE,
                AuditEntityType.TEST_PURCHASE,
                purchase.getId(),
                AuditChangeSummary.join(
                        AuditChangeSummary.fieldChange("visitAppointmentAt", previousAt, scheduledAt),
                        AuditChangeSummary.kv("confirmed", confirmed),
                        AuditChangeSummary.kv("locationPresent", purchase.getVisitAppointmentLocation() != null)));

        if (confirmed && (!wasConfirmed || previousAt == null || !previousAt.equals(scheduledAt))) {
            notifyParticipants(purchase, scheduledAt, location, notes, true);
        } else if (!confirmed) {
            notifyParticipants(purchase, scheduledAt, location, notes, false);
        }
    }

    @Override
    @Transactional
    public void confirm(UUID testPurchaseId) {
        TestPurchase purchase = loadForAppointmentChange(testPurchaseId);
        accessControlService.checkCanManageVisitAppointment(purchase);

        if (purchase.getVisitAppointmentAt() == null) {
            throw new IllegalStateException("No visit appointment to confirm");
        }
        if (purchase.isVisitAppointmentConfirmed()) {
            return;
        }
        if (purchase.getVisitAppointmentAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot confirm a visit appointment in the past");
        }

        purchase.setVisitAppointmentConfirmed(true);
        purchase.setUpdatedByUser(SecurityUtils.getCurrentUsername());
        testPurchaseRepository.save(purchase);

        auditLogService.record(
                AuditAction.APPOINTMENT_CONFIRM,
                AuditEntityType.TEST_PURCHASE,
                purchase.getId(),
                AuditChangeSummary.join(
                        AuditChangeSummary.kv("visitAppointmentAt", purchase.getVisitAppointmentAt()),
                        AuditChangeSummary.kv("confirmed", true)));

        notifyParticipants(
                purchase,
                purchase.getVisitAppointmentAt(),
                purchase.getVisitAppointmentLocation(),
                purchase.getVisitAppointmentNotes(),
                true);
    }

    private TestPurchase loadForAppointmentChange(UUID testPurchaseId) {
        TestPurchase purchase = testPurchaseRepository.findByIdWithRelations(testPurchaseId)
                .orElseThrow(() -> new EntityNotFoundException("TestPurchase not found"));
        ensureAppointmentAllowed(purchase);
        return purchase;
    }

    private static void ensureAppointmentAllowed(TestPurchase purchase) {
        if (purchase.getStatus().isArchived()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Archived test purchases are read-only");
        }
        if (purchase.getStatus() == TestPurchaseStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot schedule a visit for a cancelled test purchase");
        }
    }

    private boolean resolveConfirmedOnSave(User currentUser, boolean confirmRequested) {
        Role role = currentUser.getRole();
        if (role == Role.ADMIN || role == Role.ACCOUNT_MANAGER) {
            return true;
        }
        return confirmRequested;
    }

    private void notifyParticipants(TestPurchase purchase,
                                    LocalDateTime scheduledAt,
                                    String location,
                                    String notes,
                                    boolean confirmed) {
        if (purchase.getMysteryShopper() != null && purchase.getMysteryShopper().getEmail() != null) {
            emailService.sendVisitAppointmentEmail(
                    purchase.getMysteryShopper().getEmail(),
                    purchase.getNumber(),
                    scheduledAt,
                    location,
                    notes,
                    confirmed);
        }
        if (purchase.getCustomer() != null
                && purchase.getCustomer().getEmail() != null
                && !purchase.getCustomer().getEmail().isBlank()) {
            emailService.sendVisitAppointmentEmail(
                    purchase.getCustomer().getEmail(),
                    purchase.getNumber(),
                    scheduledAt,
                    location,
                    notes,
                    confirmed);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
