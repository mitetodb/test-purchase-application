package app.repository;

import app.model.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    @Query(
            value = "SELECT COALESCE(MAX(CAST(number AS UNSIGNED)), 0) FROM customers",
            nativeQuery = true
    )
    Long findLastSequence();
}
