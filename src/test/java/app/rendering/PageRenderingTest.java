package app.rendering;

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
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive rendering tests for ALL Thymeleaf pages with EVERY role.
 * Tests run with open-in-view=false to match production.
 * NOT @Transactional to catch lazy-loading issues.
 */
@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PageRenderingTest {

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

    private Customer customer1;
    private Customer customer2;
    private Shop shop1;
    private TestPurchase testPurchase1;
    private TestPurchase testPurchase2;

    @BeforeEach
    void setup() {
        // Clean up data for this test
        cleanupDatabase();
        setupData();
    }

    @org.springframework.transaction.annotation.Transactional
    private void setupData() {
        // Create users
        adminUser = createUser("admin_render", Role.ADMIN, null);
        accountManagerUser = createUser("accmgr_render", Role.ACCOUNT_MANAGER, null);
        salesManagerUser = createUser("salesmgr_render", Role.SALES_MANAGER, null);
        customerUser = createUser("customer_render", Role.CUSTOMER, null);
        mysteryShopperUser = createUser("mystery_render", Role.MYSTERY_SHOPPER, null);

        // Create shop
        shop1 = createShop("Shop Render Test");

        // Create customers
        customer1 = createCustomer("Customer 1 Render", accountManagerUser, salesManagerUser);
        customer2 = createCustomer("Customer 2 Render", null, null);

        // Link account manager to customer1
        accountManagerUser.getManagedCustomers().add(customer1);
        userRepository.save(accountManagerUser);

        // Link customer user to customer1
        customerUser.setCustomer(customer1);
        userRepository.save(customerUser);

        // Create test purchases
        testPurchase1 = createTestPurchase("TP-RENDER-001", customer1, mysteryShopperUser);
        testPurchase2 = createTestPurchase("TP-RENDER-002", customer2, null);
    }

    private User createUser(String username, Role role, Customer customer) {
        User user = User.builder()
                .username(username)
                .email(username + "@test.com")
                .password(passwordEncoder.encode("password"))
                .role(role)
                .active(true)
                .country(Country.GERMANY)
                .customer(customer)
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

    @org.springframework.transaction.annotation.Transactional
    private void cleanupDatabase() {
        testPurchaseRepository.deleteAll();
        testPurchaseRepository.flush();
        userRepository.clearAllAccountManagerCustomerRelationships();
        userRepository.flush();
        customerRepository.deleteAll();
        customerRepository.flush();
        shopRepository.deleteAll();
        shopRepository.flush();
        userRepository.deleteAll();
        userRepository.flush();
    }

    private TestPurchase createTestPurchase(String number, Customer customer, User mysteryShopper) {
        TestPurchase tp = TestPurchase.builder()
                .number(number)
                .customer(customer)
                .shop(shop1)
                .mysteryShopper(mysteryShopper)
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

    // ==================== HOME PAGE ====================

    @Test
    void homePageRendersForAnonymous() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "admin_render", roles = {"ADMIN"})
    void homePageRendersForAdmin() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "accmgr_render", roles = {"ACCOUNT_MANAGER"})
    void homePageRendersForAccountManager() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "salesmgr_render", roles = {"SALES_MANAGER"})
    void homePageRendersForSalesManager() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "customer_render", roles = {"CUSTOMER"})
    void homePageRendersForCustomer() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "mystery_render", roles = {"MYSTERY_SHOPPER"})
    void homePageRendersForMysteryShopper() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    // ==================== DASHBOARD ====================

    @Test
    @WithMockUser(username = "admin_render", roles = {"ADMIN"})
    void dashboardRendersForAdmin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "accmgr_render", roles = {"ACCOUNT_MANAGER"})
    void dashboardRendersForAccountManager() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "salesmgr_render", roles = {"SALES_MANAGER"})
    void dashboardRendersForSalesManager() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "customer_render", roles = {"CUSTOMER"})
    void dashboardRendersForCustomer() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "mystery_render", roles = {"MYSTERY_SHOPPER"})
    void dashboardRendersForMysteryShopper() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    // ==================== TEST PURCHASES LIST ====================

    @Test
    @WithMockUser(username = "admin_render", roles = {"ADMIN"})
    void testPurchasesListRendersForAdmin() throws Exception {
        mockMvc.perform(get("/testpurchases"))
                .andExpect(status().isOk())
                .andExpect(view().name("testpurchases/testpurchases-list"))
                .andExpect(content().string(not(containsString("error-page"))))
                .andExpect(content().string(containsString("Test Purchases")));
    }

    @Test
    @WithMockUser(username = "accmgr_render", roles = {"ACCOUNT_MANAGER"})
    void testPurchasesListRendersForAccountManager() throws Exception {
        mockMvc.perform(get("/testpurchases"))
                .andExpect(status().isOk())
                .andExpect(view().name("testpurchases/testpurchases-list"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "salesmgr_render", roles = {"SALES_MANAGER"})
    void testPurchasesListRendersForSalesManager() throws Exception {
        mockMvc.perform(get("/testpurchases"))
                .andExpect(status().isOk())
                .andExpect(view().name("testpurchases/testpurchases-list"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "customer_render", roles = {"CUSTOMER"})
    void testPurchasesListRendersForCustomer() throws Exception {
        mockMvc.perform(get("/testpurchases"))
                .andExpect(status().isOk())
                .andExpect(view().name("testpurchases/testpurchases-list"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "mystery_render", roles = {"MYSTERY_SHOPPER"})
    void testPurchasesListRendersForMysteryShopper() throws Exception {
        mockMvc.perform(get("/testpurchases"))
                .andExpect(status().isOk())
                .andExpect(view().name("testpurchases/testpurchases-list"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    // ==================== TEST PURCHASE DETAIL ====================

    @Test
    @WithMockUser(username = "admin_render", roles = {"ADMIN"})
    void testPurchaseDetailRendersForAdmin() throws Exception {
        mockMvc.perform(get("/testpurchases/view/" + testPurchase1.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("testpurchases/testpurchase-view"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "accmgr_render", roles = {"ACCOUNT_MANAGER"})
    void testPurchaseDetailRendersForAccountManager() throws Exception {
        mockMvc.perform(get("/testpurchases/view/" + testPurchase1.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("testpurchases/testpurchase-view"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "mystery_render", roles = {"MYSTERY_SHOPPER"})
    void testPurchaseDetailRendersForMysteryShopper() throws Exception {
        mockMvc.perform(get("/testpurchases/view/" + testPurchase1.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("testpurchases/testpurchase-view"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "customer_render", roles = {"CUSTOMER"})
    void testPurchaseDetailRendersForCustomer() throws Exception {
        mockMvc.perform(get("/testpurchases/view/" + testPurchase1.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("testpurchases/testpurchase-view"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    // ==================== CUSTOMERS LIST ====================

    @Test
    @WithMockUser(username = "admin_render", roles = {"ADMIN"})
    void customersListRendersForAdmin() throws Exception {
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/customers-list"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "accmgr_render", roles = {"ACCOUNT_MANAGER"})
    void customersListRendersForAccountManager() throws Exception {
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/customers-list"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "salesmgr_render", roles = {"SALES_MANAGER"})
    void customersListRendersForSalesManager() throws Exception {
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/customers-list"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "customer_render", roles = {"CUSTOMER"})
    void customersListForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/customers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mystery_render", roles = {"MYSTERY_SHOPPER"})
    void customersListForbiddenForMysteryShopper() throws Exception {
        mockMvc.perform(get("/customers"))
                .andExpect(status().isForbidden());
    }

    // ==================== CUSTOMER DETAIL/EDIT ====================

    @Test
    @WithMockUser(username = "admin_render", roles = {"ADMIN"})
    void customerEditRendersForAdmin() throws Exception {
        mockMvc.perform(get("/customers/edit/" + customer1.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/customers-edit"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "accmgr_render", roles = {"ACCOUNT_MANAGER"})
    void customerEditRendersForAccountManager() throws Exception {
        mockMvc.perform(get("/customers/edit/" + customer1.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/customers-edit"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    // ==================== SHOPS LIST ====================

    @Test
    @WithMockUser(username = "admin_render", roles = {"ADMIN"})
    void shopsListRendersForAdmin() throws Exception {
        mockMvc.perform(get("/shops"))
                .andExpect(status().isOk())
                .andExpect(view().name("shops/shops-list"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "accmgr_render", roles = {"ACCOUNT_MANAGER"})
    void shopsListRendersForAccountManager() throws Exception {
        mockMvc.perform(get("/shops"))
                .andExpect(status().isOk())
                .andExpect(view().name("shops/shops-list"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "salesmgr_render", roles = {"SALES_MANAGER"})
    void shopsListForbiddenForSalesManager() throws Exception {
        mockMvc.perform(get("/shops"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer_render", roles = {"CUSTOMER"})
    void shopsListForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/shops"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mystery_render", roles = {"MYSTERY_SHOPPER"})
    void shopsListForbiddenForMysteryShopper() throws Exception {
        mockMvc.perform(get("/shops"))
                .andExpect(status().isForbidden());
    }

    // ==================== SHOP DETAIL/EDIT ====================

    @Test
    @WithMockUser(username = "admin_render", roles = {"ADMIN"})
    void shopEditRendersForAdmin() throws Exception {
        mockMvc.perform(get("/shops/edit/" + shop1.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("shops/shops-edit"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "accmgr_render", roles = {"ACCOUNT_MANAGER"})
    void shopEditRendersForAccountManager() throws Exception {
        mockMvc.perform(get("/shops/edit/" + shop1.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("shops/shops-edit"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    // ==================== USERS LIST ====================

    @Test
    @WithMockUser(username = "admin_render", roles = {"ADMIN"})
    void usersListRendersForAdmin() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/users-list"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "accmgr_render", roles = {"ACCOUNT_MANAGER"})
    void usersListForbiddenForAccountManager() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "salesmgr_render", roles = {"SALES_MANAGER"})
    void usersListForbiddenForSalesManager() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer_render", roles = {"CUSTOMER"})
    void usersListForbiddenForCustomer() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mystery_render", roles = {"MYSTERY_SHOPPER"})
    void usersListForbiddenForMysteryShopper() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isForbidden());
    }

    // ==================== INVITES ====================

    @Test
    @WithMockUser(username = "admin_render", roles = {"ADMIN"})
    void invitesPageRendersForAdmin() throws Exception {
        mockMvc.perform(get("/invites"))
                .andExpect(status().isOk())
                .andExpect(view().name("invites/invite-create"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "accmgr_render", roles = {"ACCOUNT_MANAGER"})
    void invitesPageRendersForAccountManager() throws Exception {
        mockMvc.perform(get("/invites"))
                .andExpect(status().isOk())
                .andExpect(view().name("invites/invite-create"))
                .andExpect(content().string(not(containsString("error-page"))));
    }

    @Test
    @WithMockUser(username = "salesmgr_render", roles = {"SALES_MANAGER"})
    void invitesPageForbiddenForSalesManager() throws Exception {
        mockMvc.perform(get("/invites"))
                .andExpect(status().isForbidden());
    }
}
