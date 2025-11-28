package app.repository;

import app.model.entity.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface ShopRepository extends JpaRepository<Shop, UUID> {
    @Query(
            value = "SELECT COALESCE(MAX(CAST(number AS UNSIGNED)), 0) FROM shops",
            nativeQuery = true
    )
    Long findLastSequence();
}
