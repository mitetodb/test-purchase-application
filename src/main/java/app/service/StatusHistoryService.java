package app.service;

import app.model.entity.TestPurchase;
import app.model.entity.TestPurchaseStatusHistory;
import app.model.enums.TestPurchaseStatus;
import app.repository.TestPurchaseStatusHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class StatusHistoryService {

    private final TestPurchaseStatusHistoryRepository historyRepo;

    public StatusHistoryService(TestPurchaseStatusHistoryRepository historyRepo) {
        this.historyRepo = historyRepo;
    }

    public void recordStatusChange(
            TestPurchase tp,
            TestPurchaseStatus oldStatus,
            TestPurchaseStatus newStatus,
            String username,
            String comment) {

        TestPurchaseStatusHistory h = new TestPurchaseStatusHistory();
        h.setTestPurchase(tp);
        h.setOldStatus(oldStatus);
        h.setNewStatus(newStatus);
        h.setChangedAt(LocalDateTime.now());
        h.setChangedBy(username);
        h.setComment(comment);

        historyRepo.save(h);
    }

    public List<TestPurchaseStatusHistory> getHistory(UUID tpId) {
        return historyRepo.findByTestPurchaseIdOrderByChangedAtDesc(tpId);
    }
}

