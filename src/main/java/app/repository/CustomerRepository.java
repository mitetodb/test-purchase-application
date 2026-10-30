package app.repository;

import app.model.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    @Query("""
    SELECT COALESCE(MAX(c.number), 0)
    FROM Customer c
    """)
    Long findLastSequence();

    @Query("""
        SELECT c
        FROM Customer c
        WHERE c IN (
            SELECT mc
            FROM User u
            JOIN u.managedCustomers mc
            WHERE u.id = :accountManagerId
              AND mc.active = true
        )
        """)
    List<Customer> findForAccountManager(UUID accountManagerId);

    @Query("""
        SELECT c
        FROM Customer c
        WHERE c.id = :id
          AND c IN (
              SELECT mc
              FROM User u
              JOIN u.managedCustomers mc
              WHERE u.id = :accountManagerId
                AND mc.active = true
          )
        """)
    Optional<Customer> findByIdForAccountManager(UUID id, UUID accountManagerId);

    @Query("""
        SELECT c
        FROM Customer c
        WHERE c.salesManager.id = :salesManagerId
          AND c.active = true
        """)
    List<Customer> findForSalesManager(UUID salesManagerId);

    @Query("""
        SELECT c
        FROM Customer c
        WHERE c.id = :id
          AND c.salesManager.id = :salesManagerId
          AND c.active = true
        """)
    Optional<Customer> findByIdForSalesManager(UUID id, UUID salesManagerId);

    @Query("""
        SELECT c
        FROM Customer c
        WHERE c.active = true
        """)
    List<Customer> findAllActive();

}
