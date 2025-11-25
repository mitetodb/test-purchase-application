package app.repository;

import app.model.entity.TestPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TestPurchaseRepository extends JpaRepository<TestPurchase, UUID> {
}
