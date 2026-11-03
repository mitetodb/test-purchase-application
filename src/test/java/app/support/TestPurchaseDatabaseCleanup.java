package app.support;

import app.repository.AttachmentRepository;
import app.repository.ItemRepository;
import app.repository.TestPurchaseRepository;
import app.repository.TestPurchaseStatusHistoryRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Deletes test purchase rows and dependent FK rows in a safe order for integration tests
 * that share one database across classes and execution order.
 */
public final class TestPurchaseDatabaseCleanup {

    private TestPurchaseDatabaseCleanup() {
    }

    public static void deleteAllTestPurchasesAndDependents(
            TestPurchaseStatusHistoryRepository historyRepository,
            AttachmentRepository attachmentRepository,
            ItemRepository itemRepository,
            TestPurchaseRepository testPurchaseRepository) {
        historyRepository.deleteAll();
        attachmentRepository.deleteAll();
        itemRepository.deleteAll();
        testPurchaseRepository.deleteAll();
    }

    public static void deleteTestPurchasesByIds(
            Collection<UUID> purchaseIds,
            TestPurchaseStatusHistoryRepository historyRepository,
            AttachmentRepository attachmentRepository,
            ItemRepository itemRepository,
            TestPurchaseRepository testPurchaseRepository) {
        if (purchaseIds == null || purchaseIds.isEmpty()) {
            return;
        }
        List<UUID> ids = purchaseIds.stream().distinct().toList();
        historyRepository.deleteByTestPurchaseIdIn(ids);
        for (UUID id : ids) {
            attachmentRepository.deleteAll(attachmentRepository.findByTestPurchaseId(id));
        }
        itemRepository.deleteByPurchaseIdIn(ids);
        testPurchaseRepository.deleteAllById(ids);
    }
}
