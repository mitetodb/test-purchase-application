package app.service;

import app.model.dto.ShopDTO;
import app.model.entity.Shop;
import app.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopRepository shopRepository;

    public List<Shop> findAll() {
        return shopRepository.findAll();
    }

    public Shop findById(UUID id) {
        return shopRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shop not found"));
    }

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

        return shopRepository.save(shop);
    }

    public Shop update(UUID id, ShopDTO dto) {
        Shop shop = findById(id);

        shop.setName(dto.getName());
        shop.setCountry(dto.getCountry());
        shop.setNotes(dto.getNotes());
        shop.setDescription(dto.getDescription());

        return shopRepository.save(shop);
    }

    public void delete(UUID id) {
        shopRepository.deleteById(id);
    }
}
