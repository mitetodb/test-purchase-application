package app.service;

import app.audit.AuditChangeSummary;
import app.config.SecurityUtils;
import app.exception.ResourceNotFoundException;
import app.model.dto.ShopDTO;
import app.model.entity.Shop;
import app.model.enums.AuditAction;
import app.model.enums.AuditEntityType;
import app.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;
    private final AuditLogService auditLogService;

    @Cacheable(value = "shops", key = "'all'")
    public List<Shop> findAll() {
        return shopRepository.findAll();
    }

    @Cacheable(value = "shops", key = "#id")
    public Shop findById(UUID id) {
        return shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop not found with id: " + id));
    }

    @CacheEvict(value = "shops", allEntries = true)
    @Transactional
    public Shop create(ShopDTO dto) {
        Shop shop = Shop.builder()
                .name(dto.getName())
                .country(dto.getCountry())
                .notes(dto.getNotes())
                .description(dto.getDescription())
                .build();

        Long lastSeq = shopRepository.findLastSequence();
        long nextSeq = lastSeq + 1;

        String number = String.format("%04d", nextSeq);
        shop.setNumber(number);

        shop.setUpdatedByUser(SecurityUtils.getCurrentUsername());

        Shop saved = shopRepository.save(shop);
        auditLogService.record(
                AuditAction.SHOP_CREATE,
                AuditEntityType.SHOP,
                saved.getId(),
                AuditChangeSummary.join(
                        AuditChangeSummary.kv("number", saved.getNumber()),
                        AuditChangeSummary.kv("name", saved.getName())));
        return saved;
    }

    @CacheEvict(value = "shops", allEntries = true)
    @Transactional
    public Shop update(UUID id, ShopDTO dto) {
        Shop shop = findById(id);

        String previousName = shop.getName();

        shop.setName(dto.getName());
        shop.setCountry(dto.getCountry());
        shop.setNotes(dto.getNotes());
        shop.setDescription(dto.getDescription());

        shop.setUpdatedByUser(SecurityUtils.getCurrentUsername());

        Shop saved = shopRepository.save(shop);
        auditLogService.record(
                AuditAction.SHOP_UPDATE,
                AuditEntityType.SHOP,
                saved.getId(),
                AuditChangeSummary.fieldChange("name", previousName, dto.getName()));
        return saved;
    }

    @CacheEvict(value = "shops", allEntries = true)
    public void delete(UUID id) {
        shopRepository.deleteById(id);
    }
}
