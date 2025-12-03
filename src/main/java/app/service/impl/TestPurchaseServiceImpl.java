package app.service.impl;

import app.client.PricingClient;
import app.config.SecurityUtils;
import app.model.dto.*;
import app.model.entity.Customer;
import app.model.entity.Item;
import app.model.entity.Shop;
import app.model.entity.TestPurchase;
import app.model.enums.TestPurchaseStatus;
import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import app.service.StatusHistoryService;
import app.service.TestPurchaseService;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final PricingClient pricingClient;
    private final StatusHistoryService statusHistoryService;

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
        List<Item> items = getItemList(dto.getItems(), purchase);

        purchase.setItems(items);

        // set total product price of all items
        purchase.setProductPrice(itemsTotalPrice(purchase));

        // generate unique TP number TP-1001
        Long lastSeq = testPurchaseRepository.findLastSequence();
        Long nextSeq = lastSeq + 1;

        String number = "TP-" + String.format("%04d", nextSeq);
        purchase.setNumber(number);

        // create request DTO
        PriceCalculationRequestDTO requestDTO = new PriceCalculationRequestDTO();
        requestDTO.setCustomerId(purchase.getCustomer().getId());
        requestDTO.setCountry(purchase.getCountry().name());
        requestDTO.setCategory(purchase.getCategory().name());  // S/M/L/XL
        requestDTO.setType(purchase.getType().name());          // FORWARDING_TO_CLIENT, RETURN_BACK_TO_SELLER
        requestDTO.setProductTotal(itemsTotalPrice(purchase));

        // call microservice
        PriceCalculationResponseDTO responseDTO = pricingClient.calculatePrice(requestDTO);

        // save returned prices
        purchase.setServiceFee(responseDTO.getTestPurchaseFee());
        purchase.setPostageFee(responseDTO.getPostageFee());

        purchase.setUpdatedByUser(SecurityUtils.getCurrentUsername());

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
        List<Item> newItems = getItemList(dto.getItems(), purchase);

        purchase.getItems().addAll(newItems);

        // set total product price of all items
        purchase.setProductPrice(itemsTotalPrice(purchase));

        purchase.setUpdatedByUser(SecurityUtils.getCurrentUsername());

        return testPurchaseRepository.save(purchase);
    }

    private static List<Item> getItemList(List<ItemDTO> dto, TestPurchase purchase) {
        List<Item> newItems = new ArrayList<>();
        for (ItemDTO itemDTO : dto) {
            Item item = Item.builder()
                    .productUrl(itemDTO.getProductUrl())
                    .productName(itemDTO.getProductName())
                    .quantity(itemDTO.getQuantity())
                    .unitPrice(itemDTO.getUnitPrice())
                    .purchase(purchase)
                    .build();

            newItems.add(item);
        }
        return newItems;
    }

    private static double itemsTotalPrice(TestPurchase purchase) {
        return purchase.getItems().stream()
                        .mapToDouble(i -> i.getQuantity() * i.getUnitPrice())
                        .sum();
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

    @Transactional
    public void changeStatus(UUID id, TestPurchaseStatus newStatus, String comment) {
        TestPurchase tp = testPurchaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase not found"));

        TestPurchaseStatus oldStatus = tp.getStatus();

        tp.setStatus(newStatus);
        testPurchaseRepository.save(tp);

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        statusHistoryService.recordStatusChange(tp, oldStatus, newStatus, username, comment);
    }

}
