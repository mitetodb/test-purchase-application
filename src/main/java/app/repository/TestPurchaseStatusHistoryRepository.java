package app.repository;

import app.model.entity.TestPurchaseStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TestPurchaseStatusHistoryRepository
        extends JpaRepository<TestPurchaseStatusHistory, UUID> {

    List<TestPurchaseStatusHistory> findByTestPurchaseIdOrderByChangedAtDesc(UUID id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from TestPurchaseStatusHistory h where h.testPurchase.id in :purchaseIds")
    void deleteByTestPurchaseIdIn(@Param("purchaseIds") Collection<UUID> purchaseIds);
}

