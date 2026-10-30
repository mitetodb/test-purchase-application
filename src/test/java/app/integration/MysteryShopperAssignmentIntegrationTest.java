package app.integration;

import app.TestPurchaseApplication;
import app.model.entity.*;
import app.model.enums.*;
import app.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for mystery shopper assignment.
 * Only ADMIN and ACCOUNT_MANAGER can assign mystery shoppers.
 * Uses real seeded users and data, NOT mocks.
 * NOT @Transactional to match production behavior.
 */
@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class MysteryShopperAssignmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private TestPurchaseRepository testPurchaseRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User accountManagerUser;
    private User salesManagerUser;
    private User customerUser;
    private User mysteryShopperUser;

    private Customer customer;
    private Shop shop;
    private TestPurchase testPurchase;

    @BeforeEach
    void setup() {
        setupTestData();
    }

    @org.springframework.transaction.annotation.Transactional
    void setupTestData() {
        String uuid = java.util.UUID.randomUUID().toString().substring(0, 8);
        
        // Create users with unique names
        adminUser = createUser("admin_assign_" + uuid, Role.ADMIN);
        accountManagerUser = createUser("accmgr_assign_" + uuid, Role.ACCOUNT_MANAGER);
        salesManagerUser = createUser("salesmgr_assign_" + uuid, Role.SALES_MANAGER);
        customerUser = createUser("customer_assign_" + uuid, Role.CUSTOMER);
        mysteryShopperUser = createUser("mystery_assign_" + uuid, Role.MYSTERY_SHOPPER);

        // Create shop
        shop = createShop("Shop Assignment Test " + uuid);

        // Create customer
        customer = createCustomer("Customer Assignment " + uuid, accountManagerUser, null);

        // Link account manager to customer
        accountManagerUser.getManagedCustomers().add(customer);
        accountManagerUser = userRepository.save(accountManagerUser);

        // Create test purchase
        testPurchase = createTestPurchase("TP-ASSIGN-001-" + uuid, customer);
    }

    private User createUser(String username, Role role) {
        User user = User.builder()
                .username(username)
                .email(username + "@test.com")
                .password(passwordEncoder.encode("password"))
                .role(role)
                .active(true)
                .country(Country.GERMANY)
                .managedCustomers(new HashSet<>())
                .build();
        return userRepository.save(user);
    }

    private Customer createCustomer(String name, User accountManager, User salesManager) {
        Customer customer = Customer.builder()
                .number(String.format("%05d", (int)(Math.random() * 100000)))
                .name(name)
                .country(Country.GERMANY)
                .active(true)
                .salesManager(salesManager)
                .build();
        return customerRepository.save(customer);
    }

    private Shop createShop(String name) {
        Shop shop = Shop.builder()
                .number(String.format("%05d", (int)(Math.random() * 100000)))
                .name(name)
                .country(Country.GERMANY)
                .build();
        return shopRepository.save(shop);
    }

    private TestPurchase createTestPurchase(String number, Customer customer) {
        TestPurchase tp = TestPurchase.builder()
                .number(number)
                .customer(customer)
                .shop(shop)
                .country(Country.GERMANY)
                .category(TestPurchaseCategory.M)
                .type(TestPurchaseType.RETURN_BACK_TO_SELLER)
                .status(TestPurchaseStatus.INITIALISED)
                .productPrice(100.0)
                .serviceFee(10.0)
                .postageFee(5.0)
                .active(true)
                .build();
        return testPurchaseRepository.save(tp);
    }

    // ==================== ADMIN CAN ASSIGN ====================

    @Test
    void adminCanAssignMysteryShopper() throws Exception {
        mockMvc.perform(post("/testpurchases/" + testPurchase.getId() + "/assign-mystery-shopper")
                        .with(user(adminUser.getUsername()).roles("ADMIN"))
                        .with(csrf())
                        .param("mysteryShopperId", mysteryShopperUser.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/testpurchases/view/" + testPurchase.getId()));
    }

    // ==================== ACCOUNT MANAGER CAN ASSIGN ====================

    @Test
    void accountManagerCanAssignMysteryShopper() throws Exception {
        mockMvc.perform(post("/testpurchases/" + testPurchase.getId() + "/assign-mystery-shopper")
                        .with(user(accountManagerUser.getUsername()).roles("ACCOUNT_MANAGER"))
                        .with(csrf())
                        .param("mysteryShopperId", mysteryShopperUser.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/testpurchases/view/" + testPurchase.getId()));
    }

    // ==================== SALES MANAGER CANNOT ASSIGN ====================

    @Test
    void salesManagerCannotAssignMysteryShopper() throws Exception {
        mockMvc.perform(post("/testpurchases/" + testPurchase.getId() + "/assign-mystery-shopper")
                        .with(user(salesManagerUser.getUsername()).roles("SALES_MANAGER"))
                        .with(csrf())
                        .param("mysteryShopperId", mysteryShopperUser.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // ==================== CUSTOMER CANNOT ASSIGN ====================

    @Test
    void customerCannotAssignMysteryShopper() throws Exception {
        mockMvc.perform(post("/testpurchases/" + testPurchase.getId() + "/assign-mystery-shopper")
                        .with(user(customerUser.getUsername()).roles("CUSTOMER"))
                        .with(csrf())
                        .param("mysteryShopperId", mysteryShopperUser.getId().toString()))
                .andExpect(status().isForbidden());
    }

    // ==================== MYSTERY SHOPPER CANNOT ASSIGN ====================

    @Test
    void mysteryShopperCannotAssignAnotherMysteryShopper() throws Exception {
        mockMvc.perform(post("/testpurchases/" + testPurchase.getId() + "/assign-mystery-shopper")
                        .with(user(mysteryShopperUser.getUsername()).roles("MYSTERY_SHOPPER"))
                        .with(csrf())
                        .param("mysteryShopperId", mysteryShopperUser.getId().toString()))
                .andExpect(status().isForbidden());
    }
}
