package app.service;

import java.time.LocalDateTime;
import java.util.UUID;

public interface VisitAppointmentService {

    /**
     * Schedule or update a shop visit appointment. Admin/AM may confirm in the same step;
     * mystery shoppers create a proposed slot unless {@code confirm} is true.
     */
    void scheduleOrUpdate(UUID testPurchaseId,
                          LocalDateTime scheduledAt,
                          String location,
                          String notes,
                          boolean confirm);

    /** Confirm a proposed visit appointment. */
    void confirm(UUID testPurchaseId);
}
