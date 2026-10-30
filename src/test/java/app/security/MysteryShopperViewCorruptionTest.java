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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that viewing a TestPurchase as SALES_MANAGER or CUSTOMER does not
 * corrupt the underlying entity by unsetting the mystery shopper assignment.
 */
@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
@Transactional
class MysteryShopperViewCorruptionTest {

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

    private User salesManagerUser;
    private User customerUser;
    private User mysteryShopperUser;
    private Customer customer;
    private TestPurchase testPurchase;

    @BeforeEach
    void setUp() {
        testPurchaseRepository.deleteAll();
        userRepository.deleteAll();
        customerRepository.deleteAll();

        // Create a customer
        customer = new Customer();
        customer.setName("Test Customer");
        customer.setCountry(Country.GERMANY);
        customer.setCategory(CustomerCategory.GOLD);
        customer.setActive(true);
        customer = customerRepository.save(customer);

        // Create a sales manager who owns the customer
        salesManagerUser = new User();
        salesManagerUser.setUsername("salesmgr_test");
        salesManagerUser.setPassword("password");
        salesManagerUser.setEmail("salesmgr@test.com");
        salesManagerUser.setRole(Role.SALES_MANAGER);
        salesManagerUser.setActive(true);
        salesManagerUser = userRepository.save(salesManagerUser);

        customer.setSalesManager(salesManagerUser);
        customer = customerRepository.save(customer);

        // Create a customer user linked to the customer
        customerUser = new User();
        customerUser.setUsername("customer_test");
        customerUser.setPassword("password");
        customerUser.setEmail("customer@test.com");
        customerUser.setRole(Role.CUSTOMER);
        customerUser.setCustomer(customer);
        customerUser.setActive(true);
        customerUser = userRepository.save(customerUser);

        // Create a mystery shopper
        mysteryShopperUser = new User();
        mysteryShopperUser.setUsername("mystery_test");
        mysteryShopperUser.setPassword("password");
        mysteryShopperUser.setEmail("mystery@test.com");
        mysteryShopperUser.setRole(Role.MYSTERY_SHOPPER);
        mysteryShopperUser.setActive(true);
        mysteryShopperUser = userRepository.save(mysteryShopperUser);

        // Create a shop
        Shop shop = new Shop();
        shop.setName("Test Shop");
        shop.setCountry(Country.GERMANY);
        shop = shopRepository.save(shop);

        // Create a test purchase assigned to the mystery shopper
        testPurchase = new TestPurchase();
        testPurchase.setNumber("TP-0001");
        testPurchase.setCustomer(customer);
        testPurchase.setShop(shop);
        testPurchase.setCountry(Country.GERMANY);
        testPurchase.setCategory(Category.ACCESSORIES);
        testPurchase.setType(TestPurchaseType.ONLINE);
        testPurchase.setStatus(TestPurchaseStatus.INITIALISED);
        testPurchase.setMysteryShopper(mysteryShopperUser);
        testPurchase.setActive(true);
        testPurchase.setProductPrice(100.0);
        testPurchase.setServiceFee(10.0);
        testPurchase.setPostageFee(5.0);
        testPurchase = testPurchaseRepository.save(testPurchase);
    }

    @Test
    void viewingBySalesManagerDoesNotUnassignMysteryShopperInDatabase() {
        UUID tpId = testPurchase.getId();
        UUID mysteryShopperId = mysteryShopperUser.getId();

        // Verify the assignment exists before the view
        TestPurchase beforeView = testPurchaseRepository.findById(tpId).orElseThrow();
        assertNotNull(beforeView.getMysteryShopper(), "Mystery shopper should be assigned before view");
        assertEquals(mysteryShopperId, beforeView.getMysteryShopper().getId());

        // Sales Manager views the purchase (should hide the shopper name)
        authenticateAs(salesManagerUser);
        TestPurchaseViewDTO viewDTO = testPurchaseService.findByIdForCurrentUser(tpId);

        // Verify the DTO hides the shopper name
        assertNull(viewDTO.getMysteryShopper(), "DTO should not expose mystery shopper to SALES_MANAGER");
        assertTrue(viewDTO.isMysteryShopperHidden(), "DTO should indicate shopper is hidden");

        // Verify the database assignment is still intact
        testPurchaseRepository.flush();
        testPurchaseRepository.clear();
        TestPurchase afterView = testPurchaseRepository.findById(tpId).orElseThrow();
        assertNotNull(afterView.getMysteryShopper(), "Mystery shopper should still be assigned after view");
        assertEquals(mysteryShopperId, afterView.getMysteryShopper().getId(), 
                "Mystery shopper ID should be unchanged after view");
    }

    @Test
    void viewingByCustomerDoesNotUnassignMysteryShopperInDatabase() {
        UUID tpId = testPurchase.getId();
        UUID mysteryShopperId = mysteryShopperUser.getId();

        // Verify the assignment exists before the view
        TestPurchase beforeView = testPurchaseRepository.findById(tpId).orElseThrow();
        assertNotNull(beforeView.getMysteryShopper(), "Mystery shopper should be assigned before view");
        assertEquals(mysteryShopperId, beforeView.getMysteryShopper().getId());

        // Customer views the purchase (should hide the shopper name)
        authenticateAs(customerUser);
        TestPurchaseViewDTO viewDTO = testPurchaseService.findByIdForCurrentUser(tpId);

        // Verify the DTO hides the shopper name
        assertNull(viewDTO.getMysteryShopper(), "DTO should not expose mystery shopper to CUSTOMER");
        assertTrue(viewDTO.isMysteryShopperHidden(), "DTO should indicate shopper is hidden");

        // Verify the database assignment is still intact
        testPurchaseRepository.flush();
        testPurchaseRepository.clear();
        TestPurchase afterView = testPurchaseRepository.findById(tpId).orElseThrow();
        assertNotNull(afterView.getMysteryShopper(), "Mystery shopper should still be assigned after view");
        assertEquals(mysteryShopperId, afterView.getMysteryShopper().getId(), 
                "Mystery shopper ID should be unchanged after view");
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
