package app.security;

import app.TestPurchaseApplication;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for mystery shopper assignment feature.
 * Verifies that only ADMIN and ACCOUNT_MANAGER can assign mystery shoppers.
 */
@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MysteryShopperAssignmentTest {

    @Autowired
    private MockMvc mockMvc;

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

    private TestPurchase testPurchase;
    private User mysteryShopperUser;

    @BeforeEach
    void setUp() {
        testPurchaseRepository.deleteAll();
        userRepository.deleteAll();
        customerRepository.deleteAll();

        // Create a customer
        Customer customer = new Customer();
        customer.setName("Test Customer");
        customer.setNumber("1000");
        customer.setCountry(Country.GERMANY);
        customer.setCategory("GOLD");
        customer.setActive(true);
        customer = customerRepository.save(customer);

        // Create a mystery shopper
        mysteryShopperUser = new User();
        mysteryShopperUser.setUsername("mystery_assign");
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

        // Create a test purchase
        testPurchase = new TestPurchase();
        testPurchase.setNumber("TP-0001");
        testPurchase.setCustomer(customer);
        testPurchase.setShop(shop);
        testPurchase.setCountry(Country.GERMANY);
        testPurchase.setCategory(TestPurchaseCategory.S);
        testPurchase.setType(TestPurchaseType.FORWARDING_TO_CLIENT);
        testPurchase.setStatus(TestPurchaseStatus.INITIALISED);
        testPurchase.setActive(true);
        testPurchase.setProductPrice(100.0);
        testPurchase.setServiceFee(10.0);
        testPurchase.setPostageFee(5.0);
        testPurchase = testPurchaseRepository.save(testPurchase);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminCanAssignMysteryShopperViaController() throws Exception {
        mockMvc.perform(post("/testpurchases/" + testPurchase.getId() + "/assign-mystery-shopper")
                        .param("mysteryShopperId", mysteryShopperUser.getId().toString())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        TestPurchase updated = testPurchaseRepository.findById(testPurchase.getId()).orElseThrow();
        assertNotNull(updated.getMysteryShopper());
        assertEquals(mysteryShopperUser.getId(), updated.getMysteryShopper().getId());
    }

    @Test
    @WithMockUser(username = "accountmgr", roles = {"ACCOUNT_MANAGER"})
    void accountManagerCanAssignMysteryShopperViaController() throws Exception {
        mockMvc.perform(post("/testpurchases/" + testPurchase.getId() + "/assign-mystery-shopper")
                        .param("mysteryShopperId", mysteryShopperUser.getId().toString())
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        TestPurchase updated = testPurchaseRepository.findById(testPurchase.getId()).orElseThrow();
        assertNotNull(updated.getMysteryShopper());
        assertEquals(mysteryShopperUser.getId(), updated.getMysteryShopper().getId());
    }

    @Test
    @WithMockUser(username = "salesmgr", roles = {"SALES_MANAGER"})
    void salesManagerCannotAssignMysteryShopperViaController() throws Exception {
        mockMvc.perform(post("/testpurchases/" + testPurchase.getId() + "/assign-mystery-shopper")
                        .param("mysteryShopperId", mysteryShopperUser.getId().toString())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void customerCannotAssignMysteryShopperViaController() throws Exception {
        mockMvc.perform(post("/testpurchases/" + testPurchase.getId() + "/assign-mystery-shopper")
                        .param("mysteryShopperId", mysteryShopperUser.getId().toString())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mystery", roles = {"MYSTERY_SHOPPER"})
    void mysteryShopperCannotAssignMysteryShopperViaController() throws Exception {
        mockMvc.perform(post("/testpurchases/" + testPurchase.getId() + "/assign-mystery-shopper")
                        .param("mysteryShopperId", mysteryShopperUser.getId().toString())
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminCanUnassignMysteryShopperByPassingNull() throws Exception {
        // First assign
        testPurchase.setMysteryShopper(mysteryShopperUser);
        testPurchaseRepository.save(testPurchase);

        // Then unassign
        mockMvc.perform(post("/testpurchases/" + testPurchase.getId() + "/assign-mystery-shopper")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        TestPurchase updated = testPurchaseRepository.findById(testPurchase.getId()).orElseThrow();
        assertNull(updated.getMysteryShopper());
    }
}
