package app.support;

import app.repository.AttachmentRepository;
import app.repository.CustomerRepository;
import app.repository.ItemRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import app.repository.TestPurchaseStatusHistoryRepository;
import app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.UUID;

/**
 * FK-safe test purchase cleanup for integration tests sharing one in-memory database.
 */
@Component
@RequiredArgsConstructor
public class TestPurchaseTestDataCleaner {

    private final TestPurchaseStatusHistoryRepository historyRepository;
    private final AttachmentRepository attachmentRepository;
    private final ItemRepository itemRepository;
    private final TestPurchaseRepository testPurchaseRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteAllPurchasesAndDependents() {
        TestPurchaseDatabaseCleanup.deleteAllTestPurchasesAndDependents(
                historyRepository, attachmentRepository, itemRepository, testPurchaseRepository);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deletePurchasesByIds(Collection<UUID> purchaseIds) {
        TestPurchaseDatabaseCleanup.deleteTestPurchasesByIds(
                purchaseIds, historyRepository, attachmentRepository, itemRepository, testPurchaseRepository);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetPurchaseGraphAndBusinessFixtures(
            CustomerRepository customerRepository,
            UserRepository userRepository,
            ShopRepository shopRepository) {
        deleteAllPurchasesAndDependents();
        customerRepository.deleteAll();
        userRepository.deleteAll();
        shopRepository.deleteAll();
    }
}
