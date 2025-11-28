package app.service.impl;

import app.client.PriceCalculationClient;
import app.model.dto.ItemDTO;
import app.model.dto.TestPurchaseCreateDTO;
import app.model.dto.TestPurchaseEditDTO;
import app.model.entity.Customer;
import app.model.entity.Item;
import app.model.entity.Shop;
import app.model.entity.TestPurchase;
import app.model.enums.TestPurchaseStatus;
import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import app.service.TestPurchaseService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TestPurchaseServiceImpl implements TestPurchaseService {

    private final TestPurchaseRepository testPurchaseRepository;
    private final CustomerRepository customerRepository;
    private final ShopRepository shopRepository;
    private final PriceCalculationClient priceClient;

    @Override
    @Transactional
    public TestPurchase create(TestPurchaseCreateDTO dto) {

        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        Shop shop = shopRepository.findById(dto.getShopId())
                .orElseThrow(() -> new IllegalArgumentException("Shop not found"));

        TestPurchase purchase = new TestPurchase();
        purchase.setCustomer(customer);
        purchase.setShop(shop);
        purchase.setCountry(dto.getCountry());
        purchase.setCategory(dto.getCategory());
        purchase.setType(dto.getType());
        purchase.setStatus(TestPurchaseStatus.INITIALISED);

        // convert DTO items to entities
        List<Item> items = new ArrayList<>();
        for (ItemDTO itemDTO : dto.getItems()) {
            Item item = Item.builder()
                    .productName(itemDTO.getProductName())
                    .quantity(itemDTO.getQuantity())
                    .unitPrice(itemDTO.getUnitPrice())
                    .purchase(purchase)
                    .build();

            items.add(item);
        }

        purchase.setItems(items);

        // generate unique TP number TP-1001
        Long lastSeq = testPurchaseRepository.findLastSequence();
        long nextSeq = lastSeq + 1;

        String number = "TP-" + String.format("%04d", nextSeq);
        purchase.setNumber(number);

        // calculation of total price with microservice
        //purchase.setTotalPrice(calculateTotalPriceDTO(dto));
        purchase.setTotalPrice(99.99);

        return testPurchaseRepository.save(purchase);
    }

    @Override
    @Transactional
    public TestPurchase edit(UUID id, TestPurchaseEditDTO dto) {

        TestPurchase purchase = testPurchaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found"));

        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        Shop shop = shopRepository.findById(dto.getShopId())
                .orElseThrow(() -> new IllegalArgumentException("Shop not found"));

        purchase.setCustomer(customer);
        purchase.setShop(shop);
        purchase.setCountry(dto.getCountry());
        purchase.setCategory(dto.getCategory());
        purchase.setType(dto.getType());

        // clear old items
        purchase.getItems().clear();

        // add new items
        List<Item> newItems = new ArrayList<>();
        for (ItemDTO itemDTO : dto.getItems()) {
            Item item = Item.builder()
                    .productName(itemDTO.getProductName())
                    .quantity(itemDTO.getQuantity())
                    .unitPrice(itemDTO.getUnitPrice())
                    .purchase(purchase)
                    .build();

            newItems.add(item);
        }

        purchase.getItems().addAll(newItems);

        // re-calculation of total price with microservice
        //purchase.setTotalPrice(calculateTotalPriceDTO(dto));
        purchase.setTotalPrice(99.99);
        return testPurchaseRepository.save(purchase);
    }

    @Override
    public void delete(UUID id) {
        testPurchaseRepository.deleteById(id);
    }

    @Override
    public TestPurchase findById(UUID id) {
        return testPurchaseRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found"));
    }

    @Override
    public List<TestPurchase> findAll() {
        return testPurchaseRepository.findAll();
    }

    // convert DTO to structure
    private Double calculateTotalPriceDTO(TestPurchaseCreateDTO dto) {
        Map<String, Object> body = toPriceRequest(dto.getItems());
        return priceClient.calculateTotalPrice(body);
    }

    private Double calculateTotalPriceDTO(TestPurchaseEditDTO dto) {
        Map<String, Object> body = toPriceRequest(dto.getItems());
        return priceClient.calculateTotalPrice(body);
    }

    private Map<String, Object> toPriceRequest(List<ItemDTO> items) {
        List<Map<String, Object>> itemsList = new ArrayList<>();

        for (ItemDTO item : items) {
            itemsList.add(Map.of(
                    "productName", item.getProductName(),
                    "quantity", item.getQuantity(),
                    "unitPrice", item.getUnitPrice()
            ));
        }

        return Map.of("items", itemsList);
    }
}
