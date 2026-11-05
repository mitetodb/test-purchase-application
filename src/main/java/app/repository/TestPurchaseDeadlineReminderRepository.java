package app.repository;

import app.model.entity.TestPurchaseDeadlineReminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TestPurchaseDeadlineReminderRepository extends JpaRepository<TestPurchaseDeadlineReminder, UUID> {

    boolean existsByTestPurchase_IdAndDaysBeforeOffset(UUID testPurchaseId, int daysBeforeOffset);
}
