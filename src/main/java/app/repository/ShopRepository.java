package app.repository;

import app.model.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface ShopRepository extends JpaRepository<Shop, UUID> {
    @Query("""
    SELECT COALESCE(MAX(c.number), 0)
    FROM Shop c
    """)
    Long findLastSequence();

    @Query("""
        SELECT s FROM Shop s
        WHERE s.active = true
        ORDER BY s.number
        """)
    List<Shop> findAllActive();

    @Query("""
        SELECT s FROM Shop s
        ORDER BY s.number
        """)
    List<Shop> findAllOrdered();

    long countByActiveTrue();
}
