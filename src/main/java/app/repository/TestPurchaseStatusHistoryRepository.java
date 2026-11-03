package app.repository;

import app.model.entity.TestPurchaseStatusHistory;
import app.model.enums.TestPurchaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestPurchaseStatusHistoryRepository
        extends JpaRepository<TestPurchaseStatusHistory, UUID> {

    List<TestPurchaseStatusHistory> findByTestPurchaseIdOrderByChangedAtDesc(UUID id);

    Optional<TestPurchaseStatusHistory> findFirstByTestPurchaseIdAndNewStatusOrderByChangedAtDesc(
            UUID testPurchaseId,
            TestPurchaseStatus newStatus);
}
