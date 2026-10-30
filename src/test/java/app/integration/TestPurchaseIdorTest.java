package app.integration;

import app.TestPurchaseApplication;
import app.model.dto.ItemDTO;
import app.model.dto.PriceCalculationRequestDTO;
import app.model.dto.TestPurchaseCreateDTO;
import app.model.entity.*;
import app.model.enums.*;
import app.repository.*;
import app.service.TestPurchaseService;
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
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for cross-company and cross-Account-Manager IDOR protection.
 * Uses real seeded users and data, NOT mocks of AccessControlService.
 * Uses unique test data to avoid collisions.
 */
@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TestPurchaseIdorTest {

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

    @Autowired
    private TestPurchaseService testPurchaseService;

    private User accountManager1;
    private User accountManager2;
    private User salesManager1;
    private User salesManager2;
    private User customerUser1;
    private User customerUser2;

    private Customer customer1;
    private Customer customer2;
    private Shop shop;
    private TestPurchase testPurchase1;
    private TestPurchase testPurchase2;

    @BeforeEach
    void setup() {
        setupTestData();
    }

    @org.springframework.transaction.annotation.Transactional
    void setupTestData() {
        String uuid = Integer.toHexString((int)(Math.random() * 65536)); // 4-char hex
        
        // Create account managers
        accountManager1 = createUser("accmgr1_idor_" + uuid, Role.ACCOUNT_MANAGER);
        accountManager2 = createUser("accmgr2_idor_" + uuid, Role.ACCOUNT_MANAGER);

        // Create sales managers
        salesManager1 = createUser("salesmgr1_idor_" + uuid, Role.SALES_MANAGER);
        salesManager2 = createUser("salesmgr2_idor_" + uuid, Role.SALES_MANAGER);

        // Create shop
        shop = createShop("Shop IDOR Test " + uuid);

        // Create customers - customer1 managed by accountManager1 and salesManager1
        customer1 = createCustomer("Customer 1 IDOR " + uuid, accountManager1, salesManager1);
        customer2 = createCustomer("Customer 2 IDOR " + uuid, accountManager2, salesManager2);

        // Link managers to customers in many-to-many relationship
        accountManager1.getManagedCustomers().add(customer1);
        accountManager1 = userRepository.save(accountManager1);

        accountManager2.getManagedCustomers().add(customer2);
        accountManager2 = userRepository.save(accountManager2);

        // Create customer role users
        customerUser1 = createUser("customer1_idor_" + uuid, Role.CUSTOMER);
        customerUser1.setCustomer(customer1);
        userRepository.save(customerUser1);

        customerUser2 = createUser("customer2_idor_" + uuid, Role.CUSTOMER);
        customerUser2.setCustomer(customer2);
        userRepository.save(customerUser2);

        // Create test purchases with properly formatted numbers (TP-XXXX where XXXX is integer)
        int num1 = (int)(Math.random() * 9000) + 1000; // Random 4-digit number
        int num2 = num1 + 1;
        testPurchase1 = createTestPurchase("TP-" + num1, customer1);
        testPurchase2 = createTestPurchase("TP-" + num2, customer2);
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

    // ==================== VIEW IDOR TESTS ====================

    @Test
    void accountManager1CannotViewTestPurchaseFromCustomer2() throws Exception {
        mockMvc.perform(get("/testpurchases/view/" + testPurchase2.getId())
                        .with(user(accountManager1.getUsername()).roles("ACCOUNT_MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountManager2CannotViewTestPurchaseFromCustomer1() throws Exception {
        mockMvc.perform(get("/testpurchases/view/" + testPurchase1.getId())
                        .with(user(accountManager2.getUsername()).roles("ACCOUNT_MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void salesManager1CannotViewTestPurchaseFromCustomer2() throws Exception {
        mockMvc.perform(get("/testpurchases/view/" + testPurchase2.getId())
                        .with(user(salesManager1.getUsername()).roles("SALES_MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void salesManager2CannotViewTestPurchaseFromCustomer1() throws Exception {
        mockMvc.perform(get("/testpurchases/view/" + testPurchase1.getId())
                        .with(user(salesManager2.getUsername()).roles("SALES_MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerUser1CannotViewTestPurchaseFromCustomer2() throws Exception {
        mockMvc.perform(get("/testpurchases/view/" + testPurchase2.getId())
                        .with(user(customerUser1.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerUser2CannotViewTestPurchaseFromCustomer1() throws Exception {
        mockMvc.perform(get("/testpurchases/view/" + testPurchase1.getId())
                        .with(user(customerUser2.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountManager1CanViewTheirOwnTestPurchase() throws Exception {
        mockMvc.perform(get("/testpurchases/view/" + testPurchase1.getId())
                        .with(user(accountManager1.getUsername()).roles("ACCOUNT_MANAGER")))
                .andExpect(status().isOk());
    }

    @Test
    void salesManager1CanViewTheirOwnTestPurchase() throws Exception {
        mockMvc.perform(get("/testpurchases/view/" + testPurchase1.getId())
                        .with(user(salesManager1.getUsername()).roles("SALES_MANAGER")))
                .andExpect(status().isOk());
    }

    // ==================== EDIT IDOR TESTS ====================

    @Test
    void accountManager1CannotEditTestPurchaseFromCustomer2() throws Exception {
        // Verify setup: accountManager1 should manage customer1, not customer2
        User am1 = userRepository.findByIdWithManagedCustomers(accountManager1.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(
            am1.getManagedCustomers().stream().anyMatch(c -> c.getId().equals(customer1.getId())),
            "AccountManager1 should manage customer1"
        );
        org.junit.jupiter.api.Assertions.assertFalse(
            am1.getManagedCustomers().stream().anyMatch(c -> c.getId().equals(customer2.getId())),
            "AccountManager1 should NOT manage customer2"
        );
        
        mockMvc.perform(get("/testpurchases/edit/" + testPurchase2.getId())
                        .with(user(accountManager1.getUsername()).roles("ACCOUNT_MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountManager2CannotEditTestPurchaseFromCustomer1() throws Exception {
        mockMvc.perform(get("/testpurchases/edit/" + testPurchase1.getId())
                        .with(user(accountManager2.getUsername()).roles("ACCOUNT_MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountManager1CanEditTheirOwnTestPurchase() throws Exception {
        mockMvc.perform(get("/testpurchases/edit/" + testPurchase1.getId())
                        .with(user(accountManager1.getUsername()).roles("ACCOUNT_MANAGER")))
                .andExpect(status().isOk());
    }

    // ==================== CUSTOMER IDOR TESTS ====================

    @Test
    void accountManager1CannotViewCustomer2() throws Exception {
        mockMvc.perform(get("/customers/edit/" + customer2.getId())
                        .with(user(accountManager1.getUsername()).roles("ACCOUNT_MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountManager2CannotViewCustomer1() throws Exception {
        mockMvc.perform(get("/customers/edit/" + customer1.getId())
                        .with(user(accountManager2.getUsername()).roles("ACCOUNT_MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void salesManager1CannotAccessCustomerEditPage() throws Exception {
        // Sales managers can view customers but cannot edit them
        mockMvc.perform(get("/customers/edit/" + customer1.getId())
                        .with(user(salesManager1.getUsername()).roles("SALES_MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountManager1CanViewTheirOwnCustomer() throws Exception {
        mockMvc.perform(get("/customers/edit/" + customer1.getId())
                        .with(user(accountManager1.getUsername()).roles("ACCOUNT_MANAGER")))
                .andExpect(status().isOk());
    }
}
