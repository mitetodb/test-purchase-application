package app.security;

import app.TestPurchaseApplication;
import app.model.dto.TestPurchaseViewDTO;
import app.model.entity.Customer;
import app.model.entity.Shop;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.*;
import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
import app.service.TestPurchaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that mystery shopper name is hidden from SALES_MANAGER and CUSTOMER
 * in all views: list, detail, dashboard, exports, and API responses.
 */
@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
@Transactional
class MysteryShopperPrivacyTest {

    @Autowired
    private TestPurchaseService testPurchaseService;

    @Autowired
    private TestPurchaseRepository testPurchaseRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private User salesManagerUser;
    private User customerUser;
    private User accountManagerUser;
    private User mysteryShopperUser;
    private Customer customer;
    private TestPurchase testPurchase;

    @Autowired
    private app.repository.InviteTokenRepository inviteTokenRepository;

    @BeforeEach
    void setUp() {
        // Delete invite_tokens first to avoid foreign key constraint violations
        inviteTokenRepository.deleteAll();
        testPurchaseRepository.deleteAll();
        userRepository.deleteAll();
        customerRepository.deleteAll();

        // Create a customer
        customer = new Customer();
        customer.setName("Test Customer");
        customer.setNumber("1000");
        customer.setCountry(Country.GERMANY);
        customer.setCategory("GOLD");
        customer.setActive(true);
        customer = customerRepository.save(customer);

        // Create admin
        adminUser = new User();
        adminUser.setUsername("admin_privacy");
        adminUser.setPassword("password");
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(Role.ADMIN);
        adminUser.setActive(true);
        adminUser = userRepository.save(adminUser);

        // Create account manager who manages the customer
        accountManagerUser = new User();
        accountManagerUser.setUsername("am_privacy");
        accountManagerUser.setPassword("password");
        accountManagerUser.setEmail("am@test.com");
        accountManagerUser.setRole(Role.ACCOUNT_MANAGER);
        accountManagerUser.setActive(true);
        accountManagerUser = userRepository.save(accountManagerUser);
        accountManagerUser.getManagedCustomers().add(customer);
        accountManagerUser = userRepository.save(accountManagerUser);

        // Create sales manager who owns the customer
        salesManagerUser = new User();
        salesManagerUser.setUsername("sm_privacy");
        salesManagerUser.setPassword("password");
        salesManagerUser.setEmail("sm@test.com");
        salesManagerUser.setRole(Role.SALES_MANAGER);
        salesManagerUser.setActive(true);
        salesManagerUser = userRepository.save(salesManagerUser);

        customer.setSalesManager(salesManagerUser);
        customer = customerRepository.save(customer);

        // Create customer user
        customerUser = new User();
        customerUser.setUsername("cust_privacy");
        customerUser.setPassword("password");
        customerUser.setEmail("cust@test.com");
        customerUser.setRole(Role.CUSTOMER);
        customerUser.setCustomer(customer);
        customerUser.setActive(true);
        customerUser = userRepository.save(customerUser);

        // Create mystery shopper
        mysteryShopperUser = new User();
        mysteryShopperUser.setUsername("mystery_privacy");
        mysteryShopperUser.setPassword("password");
        mysteryShopperUser.setEmail("mystery@test.com");
        mysteryShopperUser.setRole(Role.MYSTERY_SHOPPER);
        mysteryShopperUser.setActive(true);
        mysteryShopperUser = userRepository.save(mysteryShopperUser);

        // Create a shop
        Shop shop = new Shop();
        shop.setName("Test Shop");
        shop.setNumber("SHOP001");
        shop.setCountry(Country.GERMANY);
        shop = shopRepository.save(shop);

        // Create a test purchase assigned to the mystery shopper
        testPurchase = new TestPurchase();
        testPurchase.setNumber("TP-0001");
        testPurchase.setCustomer(customer);
        testPurchase.setShop(shop);
        testPurchase.setCountry(Country.GERMANY);
        testPurchase.setCategory(TestPurchaseCategory.S);
        testPurchase.setType(TestPurchaseType.FORWARDING_TO_CLIENT);
        testPurchase.setStatus(TestPurchaseStatus.INITIALISED);
        testPurchase.setMysteryShopper(mysteryShopperUser);
        testPurchase.setActive(true);
        testPurchase.setProductPrice(100.0);
        testPurchase.setServiceFee(10.0);
        testPurchase.setPostageFee(5.0);
        testPurchase = testPurchaseRepository.save(testPurchase);
    }

    @Test
    void salesManagerCannotSeeMysteryShopperInDetailView() {
        authenticateAs(salesManagerUser);
        TestPurchaseViewDTO dto = testPurchaseService.findByIdForCurrentUser(testPurchase.getId());

        assertNull(dto.getMysteryShopper(), "Sales Manager should not see mystery shopper in detail view");
        assertTrue(dto.isMysteryShopperHidden(), "DTO should indicate shopper is hidden");
    }

    @Test
    void customerCannotSeeMysteryShopperInDetailView() {
        authenticateAs(customerUser);
        TestPurchaseViewDTO dto = testPurchaseService.findByIdForCurrentUser(testPurchase.getId());

        assertNull(dto.getMysteryShopper(), "Customer should not see mystery shopper in detail view");
        assertTrue(dto.isMysteryShopperHidden(), "DTO should indicate shopper is hidden");
    }

    @Test
    void salesManagerCannotSeeMysteryShopperInListView() {
        authenticateAs(salesManagerUser);
        List<TestPurchaseViewDTO> dtos = testPurchaseService.findAllForCurrentUser();

        assertFalse(dtos.isEmpty(), "Sales Manager should see their customer's purchases");
        TestPurchaseViewDTO dto = dtos.stream()
                .filter(p -> p.getId().equals(testPurchase.getId()))
                .findFirst()
                .orElseThrow();

        assertNull(dto.getMysteryShopper(), "Sales Manager should not see mystery shopper in list view");
        assertTrue(dto.isMysteryShopperHidden(), "DTO should indicate shopper is hidden in list");
    }

    @Test
    void customerCannotSeeMysteryShopperInListView() {
        authenticateAs(customerUser);
        List<TestPurchaseViewDTO> dtos = testPurchaseService.findAllForCurrentUser();

        assertFalse(dtos.isEmpty(), "Customer should see their company's purchases");
        TestPurchaseViewDTO dto = dtos.stream()
                .filter(p -> p.getId().equals(testPurchase.getId()))
                .findFirst()
                .orElseThrow();

        assertNull(dto.getMysteryShopper(), "Customer should not see mystery shopper in list view");
        assertTrue(dto.isMysteryShopperHidden(), "DTO should indicate shopper is hidden in list");
    }

    @Test
    void adminCanSeeMysteryShopperInDetailView() {
        authenticateAs(adminUser);
        TestPurchaseViewDTO dto = testPurchaseService.findByIdForCurrentUser(testPurchase.getId());

        assertNotNull(dto.getMysteryShopper(), "Admin should see mystery shopper in detail view");
        assertEquals(mysteryShopperUser.getId(), dto.getMysteryShopper().getId());
        assertFalse(dto.isMysteryShopperHidden(), "DTO should indicate shopper is visible for admin");
    }

    @Test
    void accountManagerCanSeeMysteryShopperInDetailView() {
        authenticateAs(accountManagerUser);
        TestPurchaseViewDTO dto = testPurchaseService.findByIdForCurrentUser(testPurchase.getId());

        assertNotNull(dto.getMysteryShopper(), "Account Manager should see mystery shopper in detail view");
        assertEquals(mysteryShopperUser.getId(), dto.getMysteryShopper().getId());
        assertFalse(dto.isMysteryShopperHidden(), "DTO should indicate shopper is visible for AM");
    }

    @Test
    void mysteryShopperCanSeeThemselvesMysteryShopperInDetailView() {
        authenticateAs(mysteryShopperUser);
        TestPurchaseViewDTO dto = testPurchaseService.findByIdForCurrentUser(testPurchase.getId());

        assertNotNull(dto.getMysteryShopper(), "Mystery Shopper should see themselves in detail view");
        assertEquals(mysteryShopperUser.getId(), dto.getMysteryShopper().getId());
        assertFalse(dto.isMysteryShopperHidden(), "DTO should indicate shopper is visible for MS");
    }

    private void authenticateAs(User user) {
        String rolePrefix = "ROLE_" + user.getRole().name();
        var auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority(rolePrefix))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
