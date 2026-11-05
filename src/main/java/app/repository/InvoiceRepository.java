package app.repository;

import app.model.entity.Invoice;
import app.model.enums.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    @Query("""
            SELECT i FROM Invoice i
            LEFT JOIN FETCH i.lineItems li
            LEFT JOIN FETCH li.testPurchase
            JOIN FETCH i.customer
            WHERE i.id = :id
            """)
    Optional<Invoice> findByIdWithDetails(UUID id);

    @Query("""
            SELECT COUNT(i) > 0 FROM Invoice i
            WHERE i.customer.id = :customerId
              AND i.periodYear = :year
              AND i.periodMonth = :month
              AND i.status <> :cancelled
            """)
    boolean existsActiveInvoiceForPeriod(UUID customerId, int year, int month, InvoiceStatus cancelled);

    List<Invoice> findByCustomerIdAndPeriodYearAndPeriodMonthOrderByCreatedOnDesc(
            UUID customerId, int year, int month);
}
