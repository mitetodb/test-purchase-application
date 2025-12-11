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

    List<TestPurchase> findByMysteryShopper_Id(UUID mysteryShopperId);

    @Query("""
        SELECT DISTINCT p
        FROM TestPurchase p
        WHERE p.customer IN (
            SELECT c
            FROM User u
            JOIN u.managedCustomers c
            WHERE u.id = :accountManagerId
        )
        """)
    List<TestPurchase> findForAccountManager(UUID accountManagerId);

}
