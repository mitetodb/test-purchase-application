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
import app.service.AccessControlService;
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
    private final AccessControlService accessControlService;

    @Override
    @Transactional
    public TestPurchase create(TestPurchaseCreateDTO dto) {
        accessControlService.checkCanCreateTestPurchase();

        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        if (!customer.isActive()) {
            throw new IllegalArgumentException("Customer is inactive");
        }

        User currentUser = accessControlService.getCurrentUserWithManagedCustomers();
        if (currentUser.getRole() == Role.CUSTOMER) {
            if (currentUser.getCustomer() == null || !currentUser.getCustomer().getId().equals(customer.getId())) {
                throw new AccessDeniedException("You can only create test purchases for your own company");
            }
        }

        Shop shop = shopRepository.findById(dto.getShopId())
                .orElseThrow(() -> new IllegalArgumentException("Shop not found"));

        TestPurchase purchase = new TestPurchase();
        purchase.setCustomer(customer);
        purchase.setShop(shop);
        purchase.setCountry(dto.getCountry());
        purchase.setCategory(dto.getCategory());
        purchase.setType(dto.getType());
        purchase.setStatus(TestPurchaseStatus.INITIALISED);
        purchase.setActive(true);

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
        accessControlService.checkCanCreateTestPurchase();

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

        accessControlService.checkCanEditTestPurchase(purchase);

        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        if (!customer.isActive()) {
            throw new IllegalArgumentException("Customer is inactive");
        }

        Shop shop = shopRepository.findById(dto.getShopId())
                .orElseThrow(() -> new IllegalArgumentException("Shop not found"));

        purchase.setCustomer(customer);
        purchase.setShop(shop);
        purchase.setCountry(dto.getCountry());
        purchase.setCategory(dto.getCategory());
        purchase.setType(dto.getType());

        purchase.getItems().clear();

        List<Item> newItems = getItemList(dto.getItems(), purchase);

        purchase.getItems().addAll(newItems);

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
        User currentUser = accessControlService.getCurrentUser();
        TestPurchase purchase = testPurchaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found"));

        if (currentUser.getRole() == Role.ADMIN) {
            testPurchaseRepository.deleteById(id);
        } else {
            purchase.setActive(false);
            purchase.setUpdatedByUser(SecurityUtils.getCurrentUsername());
            testPurchaseRepository.save(purchase);
        }
    }

    @Override
    public TestPurchase findById(UUID id) {
        return testPurchaseRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found"));
    }

    @Override
    public List<TestPurchase> findAll() {
        return testPurchaseRepository.findAllActive();
    }

    @Override
    @Transactional
    public void changeStatus(UUID id, TestPurchaseStatus newStatus, String comment) {
        accessControlService.checkCanChangeTestPurchaseStatus();

        TestPurchase tp = testPurchaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase not found"));

        accessControlService.checkCanViewTestPurchase(tp);

        TestPurchaseStatus oldStatus = tp.getStatus();

        tp.setStatus(newStatus);
        testPurchaseRepository.save(tp);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "SYSTEM";

        statusHistoryService.recordStatusChange(tp, oldStatus, newStatus, username, comment);
    }

    @Override
    public List<TestPurchase> findAllForCurrentUser() {
        User currentUser = accessControlService.getCurrentUser();
        Role role = currentUser.getRole();

        return switch (role) {
            case ADMIN -> testPurchaseRepository.findAllActive();

            case SALES_MANAGER -> testPurchaseRepository.findForSalesManager(currentUser.getId());

            case MYSTERY_SHOPPER ->
                    testPurchaseRepository.findByMysteryShopper_Id(currentUser.getId());

            case ACCOUNT_MANAGER ->
                    testPurchaseRepository.findForAccountManager(currentUser.getId());

            case CUSTOMER -> {
                if (currentUser.getCustomer() == null) {
                    yield List.of();
                }
                yield testPurchaseRepository.findByCustomer_Id(currentUser.getCustomer().getId());
            }
        };
    }

    @Override
    @Transactional
    public TestPurchase findByIdForCurrentUser(UUID id) {
        TestPurchase purchase = testPurchaseRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new EntityNotFoundException("TestPurchase not found"));

        if (!purchase.isActive()) {
            throw new EntityNotFoundException("TestPurchase not found");
        }

        accessControlService.checkCanViewTestPurchase(purchase);

        if (accessControlService.shouldHideMysteryShopperName()) {
            purchase.setMysteryShopper(null);
        }

        return purchase;
    }

}
