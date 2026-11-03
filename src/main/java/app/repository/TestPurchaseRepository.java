package app.repository;

import app.model.entity.TestPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestPurchaseRepository extends JpaRepository<TestPurchase, UUID> {

    @Query("""
        SELECT DISTINCT p FROM TestPurchase p
        LEFT JOIN FETCH p.items
        WHERE p.id = :id
        """)
    Optional<TestPurchase> findByIdWithRelations(UUID id);

    @Query("""
        SELECT COALESCE(MAX(CAST(SUBSTRING(tp.number, 4) AS INTEGER)), 1000L)
        FROM TestPurchase tp
        """)
    Long findLastSequence();

    @Query("""
        SELECT p
        FROM TestPurchase p
        WHERE p.mysteryShopper.id = :mysteryShopperId
          AND p.status <> app.model.enums.TestPurchaseStatus.ARCHIVED
        """)
    List<TestPurchase> findByMysteryShopper_Id(UUID mysteryShopperId);

    @Query("""
        SELECT DISTINCT p
        FROM TestPurchase p
        WHERE p.customer IN (
            SELECT c
            FROM User u
            JOIN u.managedCustomers c
            WHERE u.id = :accountManagerId
              AND c.active = true
        )
        AND p.status <> app.model.enums.TestPurchaseStatus.ARCHIVED
        """)
    List<TestPurchase> findForAccountManager(UUID accountManagerId);

    @Query("""
        SELECT p
        FROM TestPurchase p
        WHERE p.customer.salesManager.id = :salesManagerId
          AND p.customer.active = true
          AND p.status <> app.model.enums.TestPurchaseStatus.ARCHIVED
        """)
    List<TestPurchase> findForSalesManager(UUID salesManagerId);

    @Query("""
        SELECT p
        FROM TestPurchase p
        WHERE p.customer.id = :customerId
          AND p.status <> app.model.enums.TestPurchaseStatus.ARCHIVED
        """)
    List<TestPurchase> findByCustomer_Id(UUID customerId);

    @Query("""
        SELECT p
        FROM TestPurchase p
        WHERE p.status <> app.model.enums.TestPurchaseStatus.ARCHIVED
        """)
    List<TestPurchase> findAllNonArchived();

    @Query("""
        SELECT p
        FROM TestPurchase p
        """)
    List<TestPurchase> findAllIncludingArchived();

    @Query("""
        SELECT COUNT(p)
        FROM TestPurchase p
        WHERE p.mysteryShopper.id = :mysteryShopperId
          AND p.status <> app.model.enums.TestPurchaseStatus.ARCHIVED
        """)
    long countByMysteryShopper_Id(UUID mysteryShopperId);

    @Query("""
        SELECT COUNT(DISTINCT p)
        FROM TestPurchase p
        WHERE p.customer IN (
            SELECT c
            FROM User u
            JOIN u.managedCustomers c
            WHERE u.id = :accountManagerId
              AND c.active = true
        )
        AND p.status <> app.model.enums.TestPurchaseStatus.ARCHIVED
        """)
    long countForAccountManager(UUID accountManagerId);

    @Query("""
        SELECT COUNT(p)
        FROM TestPurchase p
        WHERE p.customer.salesManager.id = :salesManagerId
          AND p.customer.active = true
          AND p.status <> app.model.enums.TestPurchaseStatus.ARCHIVED
        """)
    long countForSalesManager(UUID salesManagerId);

    @Query("""
        SELECT COUNT(p)
        FROM TestPurchase p
        WHERE p.customer.id = :customerId
          AND p.status <> app.model.enums.TestPurchaseStatus.ARCHIVED
        """)
    long countByCustomer_Id(UUID customerId);

    @Query("""
        SELECT COUNT(p)
        FROM TestPurchase p
        WHERE p.status <> app.model.enums.TestPurchaseStatus.ARCHIVED
        """)
    long countNonArchived();

    /** @deprecated use {@link #findAllNonArchived()} */
    @Deprecated
    default List<TestPurchase> findAllActive() {
        return findAllNonArchived();
    }
}
