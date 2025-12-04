package app.service.impl;

import app.client.PricingClient;
import app.config.SecurityUtils;
import app.model.dto.*;
import app.model.entity.*;
import app.model.enums.Role;
import app.model.enums.TestPurchaseStatus;
import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
import app.service.StatusHistoryService;
import app.service.TestPurchaseService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
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
    private final UserRepository userRepository;

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

        List<Item> items = getItemList(dto.getItems(), purchase);
        purchase.setItems(items);

        purchase.setProductPrice(itemsTotalPrice(purchase));

        Long lastSeq = testPurchaseRepository.findLastSequence();
        Long nextSeq = lastSeq + 1;
        String number = "TP-" + String.format("%04d", nextSeq);
        purchase.setNumber(number);

        PriceCalculationResponseDTO responseDTO = calculatePriceInternal(
                customer.getId(),
                purchase.getCountry().name(),
                purchase.getCategory().name(),
                purchase.getType().name(),
                itemsTotalPrice(purchase)
        );

        purchase.setServiceFee(responseDTO.getTestPurchaseFee());
        purchase.setPostageFee(responseDTO.getPostageFee());

        purchase.setUpdatedByUser(SecurityUtils.getCurrentUsername());

        return testPurchaseRepository.save(purchase);
    }

    @Override
    public PriceCalculationResponseDTO previewPrice(TestPurchaseCreateDTO dto) {

        if (dto.getCustomerId() == null ||
                dto.getShopId() == null ||
                dto.getCountry() == null ||
                dto.getCategory() == null ||
                dto.getType() == null) {
            throw new IllegalArgumentException("Customer, shop, country, category and type are required for price calculation.");
        }

        double productTotal = dto.getItems() == null
                ? 0.0
                : dto.getItems().stream()
                .filter(i -> i.getQuantity() != null && i.getUnitPrice() != null)
                .mapToDouble(i -> i.getQuantity() * i.getUnitPrice())
                .sum();

        return calculatePriceInternal(
                dto.getCustomerId(),
                dto.getCountry().name(),
                dto.getCategory().name(),
                dto.getType().name(),
                productTotal
        );
    }

    private PriceCalculationResponseDTO calculatePriceInternal(
            UUID customerId,
            String country,
            String category,
            String type,
            double productTotal
    ) {
        PriceCalculationRequestDTO requestDTO = new PriceCalculationRequestDTO();
        requestDTO.setCustomerId(customerId);
        requestDTO.setCountry(country);
        requestDTO.setCategory(category);
        requestDTO.setType(type);
        requestDTO.setProductTotal(productTotal);

        return pricingClient.calculatePrice(requestDTO);
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

    @Override
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

    @Override
    public List<TestPurchase> findAllForCurrentUser() {
        User currentUser = getCurrentUser();
        Role role = currentUser.getRole();

        return switch (role) {
            case ADMIN, SALES_MANAGER -> testPurchaseRepository.findAll();

            case MYSTERY_SHOPPER ->
                    testPurchaseRepository.findByMysteryShopper_Id(currentUser.getId());

            case ACCOUNT_MANAGER ->
                    testPurchaseRepository.findForAccountManager(currentUser.getId());

            default -> List.of();
        };
    }

    @Override
    @Transactional
    public TestPurchase findByIdForCurrentUser(UUID id) {
        TestPurchase purchase = testPurchaseRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new EntityNotFoundException("TestPurchase not found"));

        User currentUser = getCurrentUserWithManagedCustomers();
        Role role = currentUser.getRole();

        switch (role) {
            case ADMIN, SALES_MANAGER:
                return purchase;

            case MYSTERY_SHOPPER:
                if (purchase.getMysteryShopper() == null ||
                        !purchase.getMysteryShopper().getId().equals(currentUser.getId())) {
                    throw new AccessDeniedException("You are not allowed to view this test purchase");
                }
                return purchase;

            case ACCOUNT_MANAGER:
                if (purchase.getCustomer() == null ||
                        currentUser.getManagedCustomers() == null ||
                        !currentUser.getManagedCustomers().contains(purchase.getCustomer())) {
                    throw new AccessDeniedException("You are not allowed to view this test purchase");
                }
                return purchase;

            default:
                throw new AccessDeniedException("You are not allowed to view this test purchase");
        }
    }

    private User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Current user not found: " + username));
    }

    private User getCurrentUserWithManagedCustomers() {
        String username = SecurityUtils.getCurrentUsername();
        return userRepository.findByUsernameWithManagedCustomers(username)
                .orElseThrow(() -> new IllegalStateException("Current user not found: " + username));
    }

}
