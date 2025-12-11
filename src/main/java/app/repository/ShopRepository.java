package app.repository;

import app.model.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface ShopRepository extends JpaRepository<Shop, UUID> {
    @Query("""
    SELECT COALESCE(MAX(c.number), 0)
    FROM Shop c
    """)
    Long findLastSequence();
}
