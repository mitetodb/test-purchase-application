package app.repository;

import app.model.entity.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;
import java.util.UUID;

public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, UUID> {

    @Query("""
            SELECT li.testPurchase.id FROM InvoiceLineItem li
            WHERE li.testPurchase IS NOT NULL
              AND li.invoice.status <> app.model.enums.InvoiceStatus.CANCELLED
            """)
    Set<UUID> findTestPurchaseIdsOnNonCancelledInvoices();
}
