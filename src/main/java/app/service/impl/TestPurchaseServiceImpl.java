package app.service.impl;

import app.client.PricingClient;
import app.config.SecurityUtils;
import app.model.dto.*;
import app.model.entity.*;
import app.model.enums.Role;
import app.model.enums.TestPurchaseStatus;
import app.model.filter.TestPurchaseListFilter;
import app.repository.spec.TestPurchaseSpecifications;
import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
import app.audit.AuditChangeSummary;
import app.model.enums.AuditAction;
import app.model.enums.AuditEntityType;
import app.service.AccessControlService;
import app.service.AuditLogService;
import app.service.EntityNumberAllocator;
import app.service.StatusHistoryService;
import app.service.TestPurchaseService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final AuditLogService auditLogService;
    private final EntityNumberAllocator entityNumberAllocator;

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
        } else if (currentUser.getRole() == Role.ACCOUNT_MANAGER) {
            boolean customerIsManaged = currentUser.getManagedCustomers().stream()
                    .anyMatch(c -> c.getId().equals(customer.getId()));
            if (!customerIsManaged) {
                throw new AccessDeniedException("You can only create test purchases for customers you manage");
            }
        }

        Shop shop = shopRepository.findById(dto.getShopId())
                .orElseThrow(() -> new IllegalArgumentException("Shop not found"));

        if (!shop.isActive()) {
            throw new IllegalArgumentException("Shop is inactive");
        }

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

        purchase.setNumber(entityNumberAllocator.nextTestPurchaseNumber());

        PriceCalculationResponseDTO responseDTO = calculatePriceInternal(
                customer.getId(),
                purchase.getCountry().name(),
                purchase.getCategory().name(),
                purchase.getType().name(),
                itemsTotalPrice(purchase)
        );

        purchase.setServiceFee(responseDTO.getTestPurchaseFee());
        purchase.setPostageFee(responseDTO.getPostageFee());

        purchase.setDeadlineAt(dto.getDeadlineAt());
        purchase.setUpdatedByUser(SecurityUtils.getCurrentUsername());

        TestPurchase saved = testPurchaseRepository.save(purchase);
        auditLogService.record(
                AuditAction.TEST_PURCHASE_CREATE,
                AuditEntityType.TEST_PURCHASE,
                saved.getId(),
                AuditChangeSummary.join(
                        AuditChangeSummary.kv("number", saved.getNumber()),
                        AuditChangeSummary.kv("status", saved.getStatus())));
        return saved;
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

        User currentUser = accessControlService.getCurrentUserWithManagedCustomers();
        
        // CUSTOMER can only preview prices for their own company
        if (currentUser.getRole() == Role.CUSTOMER) {
            if (currentUser.getCustomer() == null || !currentUser.getCustomer().getId().equals(dto.getCustomerId())) {
                throw new AccessDeniedException("You can only preview prices for your own company");
            }
        }
        
        // ACCOUNT_MANAGER can only preview prices for managed customers
        if (currentUser.getRole() == Role.ACCOUNT_MANAGER) {
            boolean isManaged = currentUser.getManagedCustomers().stream()
                    .anyMatch(c -> c.getId().equals(dto.getCustomerId()));
            if (!isManaged) {
                throw new AccessDeniedException("You can only preview prices for customers you manage");
            }
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
        ensureNotArchived(purchase);

        Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        if (!customer.isActive()) {
            throw new IllegalArgumentException("Customer is inactive");
        }

        // If the customer is being changed, verify the Account Manager manages the new customer
        User currentUser = accessControlService.getCurrentUserWithManagedCustomers();
        if (currentUser.getRole() == Role.ACCOUNT_MANAGER) {
            boolean customerIsManaged = currentUser.getManagedCustomers().stream()
                    .anyMatch(c -> c.getId().equals(customer.getId()));
            if (!customerIsManaged) {
                throw new AccessDeniedException("You can only assign test purchases to customers you manage");
            }
        }

        Shop shop = shopRepository.findById(dto.getShopId())
                .orElseThrow(() -> new IllegalArgumentException("Shop not found"));

        if (!shop.isActive()) {
            throw new IllegalArgumentException("Shop is inactive");
        }

        purchase.setCustomer(customer);
        purchase.setShop(shop);
        purchase.setCountry(dto.getCountry());
        purchase.setCategory(dto.getCategory());
        purchase.setType(dto.getType());

        purchase.getItems().clear();

        List<Item> newItems = getItemList(dto.getItems(), purchase);

        purchase.getItems().addAll(newItems);

        purchase.setProductPrice(itemsTotalPrice(purchase));

        purchase.setDeadlineAt(dto.getDeadlineAt());
        purchase.setUpdatedByUser(SecurityUtils.getCurrentUsername());

        TestPurchase saved = testPurchaseRepository.save(purchase);
        auditLogService.record(
                AuditAction.TEST_PURCHASE_UPDATE,
                AuditEntityType.TEST_PURCHASE,
                saved.getId(),
                AuditChangeSummary.join(
                        AuditChangeSummary.kv("number", saved.getNumber()),
                        AuditChangeSummary.kv("category", saved.getCategory())));
        return saved;
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
    @Transactional
    public void archive(UUID id) {
        TestPurchase purchase = testPurchaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found"));

        accessControlService.checkCanViewTestPurchase(purchase);

        if (purchase.getStatus().isArchived()) {
            return;
        }

        TestPurchaseStatus oldStatus = purchase.getStatus();
        purchase.setStatus(TestPurchaseStatus.ARCHIVED);
        purchase.setActive(true);
        purchase.setUpdatedByUser(SecurityUtils.getCurrentUsername());
        testPurchaseRepository.save(purchase);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "SYSTEM";
        statusHistoryService.recordStatusChange(purchase, oldStatus, TestPurchaseStatus.ARCHIVED, username, null);
    }

    @Override
    @Transactional
    public void unarchive(UUID id) {
        TestPurchase purchase = testPurchaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found"));

        accessControlService.checkCanViewTestPurchase(purchase);

        if (!purchase.getStatus().isArchived()) {
            throw new IllegalStateException("Test purchase is not archived");
        }

        TestPurchaseStatus restoreStatus = statusHistoryService.getStatusBeforeArchive(id)
                .orElse(TestPurchaseStatus.INITIALISED);

        TestPurchaseStatus oldStatus = purchase.getStatus();
        purchase.setStatus(restoreStatus);
        purchase.setUpdatedByUser(SecurityUtils.getCurrentUsername());
        testPurchaseRepository.save(purchase);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "SYSTEM";
        statusHistoryService.recordStatusChange(
                purchase, oldStatus, restoreStatus, username, "Unarchived");
    }

    @Override
    public TestPurchase findById(UUID id) {
        return testPurchaseRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new IllegalArgumentException("Purchase not found"));
    }

    @Override
    public List<TestPurchase> findAll() {
        return testPurchaseRepository.findAllNonArchived();
    }

    @Override
    @Transactional
    public void changeStatus(UUID id, TestPurchaseStatus newStatus, String comment) {
        accessControlService.checkCanChangeTestPurchaseStatus();

        TestPurchase tp = testPurchaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Purchase not found"));

        accessControlService.checkCanViewTestPurchase(tp);
        ensureNotArchived(tp);

        TestPurchaseStatus oldStatus = tp.getStatus();

        if (newStatus.isArchived()) {
            throw new IllegalArgumentException("Use archive to set archived status");
        }

        tp.setStatus(newStatus);
        testPurchaseRepository.save(tp);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "SYSTEM";

        statusHistoryService.recordStatusChange(tp, oldStatus, newStatus, username, comment);

        auditLogService.record(
                AuditAction.TEST_PURCHASE_STATUS_CHANGE,
                AuditEntityType.TEST_PURCHASE,
                tp.getId(),
                AuditChangeSummary.join(
                        AuditChangeSummary.fieldChange("status", oldStatus, newStatus),
                        AuditChangeSummary.kv("commentPresent", comment != null && !comment.isBlank())));
    }

    @Override
    public List<TestPurchaseViewDTO> findAllForCurrentUser() {
        return findAllForCurrentUser(false);
    }

    @Override
    public List<TestPurchaseViewDTO> findAllForCurrentUser(boolean includeArchived) {
        User currentUser = accessControlService.getCurrentUser();
        Role role = currentUser.getRole();
        boolean hideMysteryShopperName = accessControlService.shouldHideMysteryShopperName();

        List<TestPurchase> purchases = switch (role) {
            case ADMIN -> includeArchived
                    ? testPurchaseRepository.findAllIncludingArchived()
                    : testPurchaseRepository.findAllNonArchived();

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

        return purchases.stream()
                .map(p -> TestPurchaseViewDTO.from(p, hideMysteryShopperName))
                .toList();
    }

    @Override
    public Page<TestPurchaseViewDTO> findPageForCurrentUser(TestPurchaseListFilter filter, Pageable pageable) {
        User currentUser = accessControlService.getCurrentUserWithManagedCustomers();
        boolean hideMysteryShopperName = accessControlService.shouldHideMysteryShopperName();
        var spec = listSpec(currentUser, filter);

        return testPurchaseRepository.findAll(spec, pageable)
                .map(p -> TestPurchaseViewDTO.from(p, hideMysteryShopperName));
    }

    @Override
    public long countForList(TestPurchaseListFilter filter) {
        User currentUser = accessControlService.getCurrentUserWithManagedCustomers();
        return testPurchaseRepository.count(listSpec(currentUser, filter));
    }

    @Override
    public List<TestPurchaseViewDTO> findAllForListExport(
            TestPurchaseListFilter filter, Pageable sortPageable, int maxRows) {
        User currentUser = accessControlService.getCurrentUserWithManagedCustomers();
        boolean hideMysteryShopperName = accessControlService.shouldHideMysteryShopperName();
        var spec = listSpec(currentUser, filter);
        Pageable page = PageRequest.of(0, maxRows, sortPageable.getSort());
        return testPurchaseRepository.findAll(spec, page).stream()
                .map(p -> TestPurchaseViewDTO.from(p, hideMysteryShopperName))
                .toList();
    }

    private org.springframework.data.jpa.domain.Specification<TestPurchase> listSpec(
            User currentUser, TestPurchaseListFilter filter) {
        TestPurchaseListFilter effectiveFilter = sanitizeFilter(filter, currentUser);
        return TestPurchaseSpecifications.listQuery(currentUser, effectiveFilter);
    }

    private TestPurchaseListFilter sanitizeFilter(TestPurchaseListFilter filter, User currentUser) {
        if (filter == null) {
            return new TestPurchaseListFilter();
        }
        TestPurchaseListFilter copy = new TestPurchaseListFilter();
        copy.setStatus(filter.getStatus());
        copy.setCustomerId(filter.getCustomerId());
        copy.setCreatedFrom(filter.getCreatedFrom());
        copy.setCreatedTo(filter.getCreatedTo());

        Role role = currentUser.getRole();
        if (role == Role.ADMIN || role == Role.ACCOUNT_MANAGER) {
            copy.setMysteryShopperId(filter.getMysteryShopperId());
        }
        if (role == Role.ADMIN) {
            copy.setShowArchived(filter.isShowArchived());
        }
        return copy;
    }

    @Override
    @Transactional
    public TestPurchaseViewDTO findByIdForCurrentUser(UUID id) {
        TestPurchase purchase = testPurchaseRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new EntityNotFoundException("TestPurchase not found"));

        accessControlService.checkCanViewTestPurchase(purchase);

        boolean hideMysteryShopperName = accessControlService.shouldHideMysteryShopperName();

        return TestPurchaseViewDTO.from(purchase, hideMysteryShopperName);
    }

    @Override
    @Transactional
    public TestPurchase findEntityByIdForCurrentUser(UUID id) {
        TestPurchase purchase = testPurchaseRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new EntityNotFoundException("TestPurchase not found"));
        accessControlService.checkCanViewTestPurchase(purchase);
        return purchase;
    }

    @Override
    @Transactional
    public TestPurchase findByIdForEdit(UUID id) {
        TestPurchase purchase = testPurchaseRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new EntityNotFoundException("TestPurchase not found"));

        ensureNotArchived(purchase);

        accessControlService.checkCanEditTestPurchase(purchase);

        return purchase;
    }

    @Override
    @Transactional
    public void assignMysteryShopperToTestPurchase(UUID testPurchaseId, UUID mysteryShopperId) {
        TestPurchase purchase = testPurchaseRepository.findById(testPurchaseId)
                .orElseThrow(() -> new EntityNotFoundException("TestPurchase not found"));

        ensureNotArchived(purchase);

        accessControlService.checkCanAssignMysteryShopperToTestPurchase();
        
        // Account Manager must manage the customer
        accessControlService.checkCanEditTestPurchase(purchase);

        User mysteryShopper = null;
        if (mysteryShopperId != null) {
            mysteryShopper = userRepository.findById(mysteryShopperId)
                    .orElseThrow(() -> new IllegalArgumentException("Mystery shopper not found"));

            if (mysteryShopper.getRole() != Role.MYSTERY_SHOPPER) {
                throw new IllegalArgumentException("User is not a mystery shopper");
            }

            if (!mysteryShopper.isActive()) {
                throw new IllegalArgumentException("Mystery shopper is inactive");
            }
        }

        purchase.setMysteryShopper(mysteryShopper);
        purchase.setUpdatedByUser(SecurityUtils.getCurrentUsername());
        testPurchaseRepository.save(purchase);
    }

    private static void ensureNotArchived(TestPurchase purchase) {
        if (purchase.getStatus().isArchived()) {
            throw new AccessDeniedException("Archived test purchases are read-only");
        }
    }

}
