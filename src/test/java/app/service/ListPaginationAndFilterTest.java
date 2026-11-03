package app.service;

import app.TestPurchaseApplication;
import app.model.entity.Customer;
import app.model.entity.Shop;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.*;
import app.model.filter.TestPurchaseListFilter;
import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
import app.support.ListPagingSupport;
import app.support.TestPurchaseTestDataCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
@Transactional
class ListPaginationAndFilterTest {

    @Autowired
    private TestPurchaseService testPurchaseService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private UserService userService;

    @Autowired
    private TestPurchaseRepository testPurchaseRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListPagingSupport listPagingSupport;

    @Autowired
    private TestPurchaseTestDataCleaner testPurchaseTestDataCleaner;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    private User admin;
    private User accountManager;
    private User salesManager;
    private Customer managedCustomer;
    private Customer otherCustomer;
    private User mysteryA;
    private User mysteryB;
    private Shop shop;

    @BeforeEach
    void setUp() {
        testPurchaseTestDataCleaner.resetPurchaseGraphAndBusinessFixtures(
                customerRepository, userRepository, shopRepository);

        admin = saveUser("pag_admin", Role.ADMIN);
        accountManager = saveUser("pag_accmgr", Role.ACCOUNT_MANAGER);
        salesManager = saveUser("pag_salesmgr", Role.SALES_MANAGER);
        mysteryA = saveUser("pag_ms_a", Role.MYSTERY_SHOPPER);
        mysteryB = saveUser("pag_ms_b", Role.MYSTERY_SHOPPER);

        managedCustomer = saveCustomer("Managed Co", salesManager);
        otherCustomer = saveCustomer("Other Co", salesManager);
        accountManager.getManagedCustomers().add(managedCustomer);
        userRepository.save(accountManager);

        shop = saveShop("ShopPag");

        savePurchase("TP-P1", managedCustomer, TestPurchaseStatus.INITIALISED, mysteryA,
                LocalDate.of(2026, 1, 10));
        savePurchase("TP-P2", managedCustomer, TestPurchaseStatus.CLOSED, mysteryB,
                LocalDate.of(2026, 2, 15));
        savePurchase("TP-P3", otherCustomer, TestPurchaseStatus.INITIALISED, mysteryA,
                LocalDate.of(2026, 1, 20));
    }

    @Test
    void adminPaginationRespectsMaxPageSize() {
        login(admin);
        Pageable oversized = listPagingSupport.forTestPurchases(PageRequest.of(0, 500));
        assertThat(oversized.getPageSize()).isEqualTo(100);

        Page<?> page = testPurchaseService.findPageForCurrentUser(new TestPurchaseListFilter(), oversized);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(3);
    }

    @Test
    void adminPaginationSecondPageCanBeEmpty() {
        login(admin);
        Pageable pageable = listPagingSupport.forTestPurchases(PageRequest.of(5, 2));
        Page<?> page = testPurchaseService.findPageForCurrentUser(new TestPurchaseListFilter(), pageable);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getNumber()).isEqualTo(5);
    }

    @Test
    void adminCanFilterByStatusAndCustomer() {
        login(admin);
        TestPurchaseListFilter filter = new TestPurchaseListFilter();
        filter.setStatus(TestPurchaseStatus.CLOSED);
        filter.setCustomerId(managedCustomer.getId());

        Page<?> page = testPurchaseService.findPageForCurrentUser(filter,
                listPagingSupport.forTestPurchases(PageRequest.of(0, 20)));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0)).extracting("number").isEqualTo("TP-P2");
    }

    @Test
    void adminCanFilterByMysteryShopperAndDateRange() {
        login(admin);
        TestPurchaseListFilter filter = new TestPurchaseListFilter();
        filter.setMysteryShopperId(mysteryA.getId());
        filter.setCreatedFrom(LocalDate.of(2026, 1, 1));
        filter.setCreatedTo(LocalDate.of(2026, 1, 31));

        Page<?> page = testPurchaseService.findPageForCurrentUser(filter,
                listPagingSupport.forTestPurchases(PageRequest.of(0, 20)));
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void accountManagerCannotSeeOtherCustomerEvenWithCustomerFilter() {
        login(accountManager);
        TestPurchaseListFilter filter = new TestPurchaseListFilter();
        filter.setCustomerId(otherCustomer.getId());

        Page<?> page = testPurchaseService.findPageForCurrentUser(filter,
                listPagingSupport.forTestPurchases(PageRequest.of(0, 20)));
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void accountManagerSeesOnlyManagedPurchasesWithoutFilter() {
        login(accountManager);
        Page<?> page = testPurchaseService.findPageForCurrentUser(new TestPurchaseListFilter(),
                listPagingSupport.forTestPurchases(PageRequest.of(0, 20)));
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void salesManagerMysteryShopperFilterIsIgnored() {
        login(salesManager);
        TestPurchaseListFilter filter = new TestPurchaseListFilter();
        filter.setMysteryShopperId(mysteryB.getId());
        filter.setStatus(TestPurchaseStatus.CLOSED);

        Page<?> page = testPurchaseService.findPageForCurrentUser(filter,
                listPagingSupport.forTestPurchases(PageRequest.of(0, 20)));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0)).extracting("number").isEqualTo("TP-P2");
    }

    @Test
    void salesManagerCannotExpandScopeWithOtherCustomerFilter() {
        login(salesManager);
        TestPurchaseListFilter filter = new TestPurchaseListFilter();
        filter.setCustomerId(otherCustomer.getId());

        Page<?> allForSales = testPurchaseService.findPageForCurrentUser(new TestPurchaseListFilter(),
                listPagingSupport.forTestPurchases(PageRequest.of(0, 20)));
        Page<?> filtered = testPurchaseService.findPageForCurrentUser(filter,
                listPagingSupport.forTestPurchases(PageRequest.of(0, 20)));

        assertThat(allForSales.getTotalElements()).isEqualTo(3);
        assertThat(filtered.getTotalElements()).isEqualTo(1);
    }

    @Test
    void customerListPaginationScopedForAccountManager() {
        login(accountManager);
        var filter = new app.model.filter.CustomerListFilter();
        Page<Customer> page = customerService.findPageForCurrentUser(filter,
                listPagingSupport.forCustomers(PageRequest.of(0, 10)));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getName()).isEqualTo("Managed Co");
    }

    @Test
    void userListPaginationAndRoleFilter() {
        login(admin);
        var filter = new app.model.filter.UserListFilter();
        filter.setRole(Role.MYSTERY_SHOPPER);

        Page<User> page = userService.findPageForAdmin(filter,
                listPagingSupport.forUsers(PageRequest.of(0, 20)));
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    private void login(User user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        "password",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()))));
    }

    private User saveUser(String username, Role role) {
        return userRepository.save(User.builder()
                .username(username)
                .email(username + "@pag.test")
                .password(passwordEncoder.encode("password"))
                .role(role)
                .country(Country.GERMANY)
                .active(true)
                .managedCustomers(new HashSet<>())
                .build());
    }

    private Customer saveCustomer(String name, User salesManager) {
        return customerRepository.save(Customer.builder()
                .number(String.format("%05d", Math.abs(UUID.randomUUID().hashCode() % 100000)))
                .name(name)
                .country(Country.GERMANY)
                .active(true)
                .salesManager(salesManager)
                .build());
    }

    private Shop saveShop(String name) {
        return shopRepository.save(Shop.builder()
                .number(String.format("%05d", Math.abs(UUID.randomUUID().hashCode() % 100000)))
                .name(name)
                .country(Country.GERMANY)
                .build());
    }

    private void savePurchase(String number, Customer customer, TestPurchaseStatus status,
                              User mysteryShopper, LocalDate createdOn) {
        TestPurchase tp = TestPurchase.builder()
                .number(number)
                .customer(customer)
                .shop(shop)
                .mysteryShopper(mysteryShopper)
                .country(Country.GERMANY)
                .category(TestPurchaseCategory.M)
                .type(TestPurchaseType.RETURN_BACK_TO_SELLER)
                .productPrice(10.0)
                .serviceFee(1.0)
                .postageFee(1.0)
                .active(true)
                .build();
        tp = testPurchaseRepository.saveAndFlush(tp);
        tp.setStatus(status);
        tp.setCreatedOn(createdOn.atStartOfDay());
        testPurchaseRepository.saveAndFlush(tp);
    }
}
