package app.repository;

import app.model.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;

public interface ItemRepository extends JpaRepository<Item, UUID> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Item i where i.purchase.id in :purchaseIds")
    void deleteByPurchaseIdIn(@Param("purchaseIds") Collection<UUID> purchaseIds);
}
