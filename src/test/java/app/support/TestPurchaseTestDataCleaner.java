package app.support;

import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * FK-safe test purchase cleanup for integration tests. Uses JDBC deletes so production
 * repositories stay unchanged; {@link Propagation#REQUIRES_NEW} commits cleanup outside
 * test transactions.
 */
@Component
@RequiredArgsConstructor
public class TestPurchaseTestDataCleaner {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteAllPurchasesAndDependents() {
        jdbcTemplate.update("DELETE FROM \"testpurchase_status_history\"");
        jdbcTemplate.update("DELETE FROM \"test_purchases_attachments\"");
        jdbcTemplate.update("DELETE FROM \"attachments\"");
        jdbcTemplate.update("DELETE FROM \"items\"");
        jdbcTemplate.update("DELETE FROM \"test_purchases\"");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deletePurchasesByIds(Collection<UUID> purchaseIds) {
        if (purchaseIds == null || purchaseIds.isEmpty()) {
            return;
        }
        List<UUID> ids = List.copyOf(new LinkedHashSet<>(purchaseIds));
        deleteWherePurchaseIdIn("\"testpurchase_status_history\"", "\"testpurchase_id\"", ids);
        deleteWherePurchaseIdIn("\"test_purchases_attachments\"", "\"test_purchase_id\"", ids);
        deleteWherePurchaseIdIn("\"attachments\"", "\"test_purchase_id\"", ids);
        deleteWherePurchaseIdIn("\"items\"", "\"purchase_id\"", ids);
        deleteWherePurchaseIdIn("\"test_purchases\"", "\"id\"", ids);
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

    private void deleteWherePurchaseIdIn(String table, String column, List<UUID> ids) {
        if (ids.isEmpty()) {
            return;
        }
        String placeholders = "?,".repeat(ids.size());
        placeholders = placeholders.substring(0, placeholders.length() - 1);
        jdbcTemplate.update(
                "DELETE FROM " + table + " WHERE " + column + " IN (" + placeholders + ")",
                ids.toArray());
    }
}
