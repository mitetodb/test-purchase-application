package app.security;

import app.TestPurchaseApplication;
import app.model.entity.Customer;
import app.model.entity.Shop;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.model.enums.TestPurchaseCategory;
import app.model.enums.TestPurchaseStatus;
import app.model.enums.TestPurchaseType;
import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
import app.support.TestPurchaseTestDataCleaner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class UserDeactivationMvcIntegrationTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

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
    private TestPurchaseTestDataCleaner testPurchaseTestDataCleaner;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final List<UUID> createdUserIds = new ArrayList<>();
    private final List<UUID> createdCustomerIds = new ArrayList<>();
    private final List<UUID> createdShopIds = new ArrayList<>();
    private final List<UUID> createdPurchaseIds = new ArrayList<>();

    private String suffix;
    private User targetUser;
    private User admin;
    private TestPurchase linkedPurchase;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        transactionTemplate.executeWithoutResult(status -> {
            Customer customer = customerRepository.save(Customer.builder()
                    .number("UD" + suffix)
                    .name("Customer " + suffix)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
            createdCustomerIds.add(customer.getId());

            Shop shop = shopRepository.save(Shop.builder()
                    .number("SH" + suffix)
                    .name("Shop " + suffix)
                    .country(Country.GERMANY)
                    .build());
            createdShopIds.add(shop.getId());

            targetUser = userRepository.save(User.builder()
                    .username("target_" + suffix)
                    .email("target_" + suffix + "@deact.test")
                    .password(passwordEncoder.encode("TargetPass1!"))
                    .role(Role.MYSTERY_SHOPPER)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
            createdUserIds.add(targetUser.getId());

            admin = userRepository.save(User.builder()
                    .username("admin_" + suffix)
                    .email("admin_" + suffix + "@deact.test")
                    .password(passwordEncoder.encode("AdminPass1!"))
                    .role(Role.ADMIN)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
            createdUserIds.add(admin.getId());

            linkedPurchase = testPurchaseRepository.save(TestPurchase.builder()
                    .number("TP-" + suffix)
                    .customer(customer)
                    .shop(shop)
                    .mysteryShopper(targetUser)
                    .country(Country.GERMANY)
                    .category(TestPurchaseCategory.M)
                    .type(TestPurchaseType.RETURN_BACK_TO_SELLER)
                    .status(TestPurchaseStatus.INITIALISED)
                    .productPrice(10.0)
                    .serviceFee(1.0)
                    .postageFee(1.0)
                    .active(true)
                    .build());
            createdPurchaseIds.add(linkedPurchase.getId());
        });
    }

    @AfterEach
    void tearDown() {
        testPurchaseTestDataCleaner.deletePurchasesByIds(createdPurchaseIds);
        transactionTemplate.executeWithoutResult(status -> {
            createdUserIds.forEach(userRepository::deleteById);
            createdCustomerIds.forEach(customerRepository::deleteById);
            createdShopIds.forEach(shopRepository::deleteById);
            createdPurchaseIds.clear();
            createdUserIds.clear();
            createdCustomerIds.clear();
            createdShopIds.clear();
        });
    }

    @Test
    void adminCanDeactivateUser() throws Exception {
        mockMvc.perform(post("/users/deactivate/" + targetUser.getId())
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        User refreshed = reload(targetUser.getId());
        assertFalse(refreshed.isActive());
    }

    @Test
    void adminCanReactivateUser() throws Exception {
        transactionTemplate.executeWithoutResult(status -> {
            targetUser.setActive(false);
            userRepository.save(targetUser);
        });

        mockMvc.perform(post("/users/reactivate/" + targetUser.getId())
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        assertTrue(reload(targetUser.getId()).isActive());
    }

    @Test
    void deactivatedUserGetsGenericLoginErrorEvenWithCorrectPassword() throws Exception {
        mockMvc.perform(post("/users/deactivate/" + targetUser.getId())
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", targetUser.getUsername())
                        .param("password", "TargetPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        mockMvc.perform(get("/login?error"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid username or password")));
    }

    @Test
    void nonAdminDeactivateReturns403() throws Exception {
        User accountManager = transactionTemplate.execute(status -> {
            User am = userRepository.save(User.builder()
                    .username("am_" + suffix)
                    .email("am_" + suffix + "@deact.test")
                    .password(passwordEncoder.encode("AmPass1!"))
                    .role(Role.ACCOUNT_MANAGER)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
            createdUserIds.add(am.getId());
            return am;
        });

        mockMvc.perform(post("/users/deactivate/" + targetUser.getId())
                        .with(user(accountManager.getUsername()).roles("ACCOUNT_MANAGER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        assertTrue(reload(targetUser.getId()).isActive());
    }

    @Test
    void nonAdminReactivateReturns403() throws Exception {
        transactionTemplate.executeWithoutResult(status -> {
            targetUser.setActive(false);
            userRepository.save(targetUser);
        });

        User salesManager = transactionTemplate.execute(status -> {
            User sm = userRepository.save(User.builder()
                    .username("sm_" + suffix)
                    .email("sm_" + suffix + "@deact.test")
                    .password(passwordEncoder.encode("SmPass1!"))
                    .role(Role.SALES_MANAGER)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
            createdUserIds.add(sm.getId());
            return sm;
        });

        mockMvc.perform(post("/users/reactivate/" + targetUser.getId())
                        .with(user(salesManager.getUsername()).roles("SALES_MANAGER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        assertFalse(reload(targetUser.getId()).isActive());
    }

    @Test
    void legacyUserDeleteEndpointIsGone() throws Exception {
        mockMvc.perform(post("/users/delete/" + targetUser.getId())
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deactivationPreservesUserHistory() throws Exception {
        mockMvc.perform(post("/users/deactivate/" + targetUser.getId())
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        transactionTemplate.executeWithoutResult(status -> {
            assertTrue(userRepository.findById(targetUser.getId()).isPresent());
            TestPurchase purchase = testPurchaseRepository.findById(linkedPurchase.getId()).orElseThrow();
            assertEquals(targetUser.getId(), purchase.getMysteryShopper().getId());
        });
    }

    @Test
    void deactivatedUserExistingSessionRejectedOnNextRequest() throws Exception {
        MockHttpSession targetSession = new MockHttpSession();

        mockMvc.perform(post("/login")
                        .session(targetSession)
                        .with(csrf())
                        .param("username", targetUser.getUsername())
                        .param("password", "TargetPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));

        mockMvc.perform(get("/home").session(targetSession))
                .andExpect(status().isOk());

        mockMvc.perform(post("/users/deactivate/" + targetUser.getId())
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        assertFalse(reload(targetUser.getId()).isActive(), "user must be inactive in DB before next request");

        mockMvc.perform(get("/home").session(targetSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertTrue(targetSession.isInvalid(), "Deactivated user's session should be invalidated");
    }

    @Test
    void missingUserRecordInvalidatesExistingSession() throws Exception {
        MockHttpSession targetSession = new MockHttpSession();

        mockMvc.perform(post("/login")
                        .session(targetSession)
                        .with(csrf())
                        .param("username", targetUser.getUsername())
                        .param("password", "TargetPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));

        mockMvc.perform(get("/home").session(targetSession))
                .andExpect(status().isOk());

        transactionTemplate.executeWithoutResult(status -> {
            User u = userRepository.findById(targetUser.getId()).orElseThrow();
            u.setUsername("removed_" + suffix);
            userRepository.save(u);
        });

        mockMvc.perform(get("/home").session(targetSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertTrue(targetSession.isInvalid(), "Session should be invalidated when user record is gone");
    }

    @Test
    void usersListShowsDeactivateAndReactivateNotDelete() throws Exception {
        mockMvc.perform(get("/users")
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/users/deactivate/")))
                .andExpect(content().string(containsString("Deactivate")))
                .andExpect(content().string(not(containsString("/users/delete/"))))
                .andExpect(content().string(not(containsString(">Delete<"))));
    }

    private User reload(UUID id) {
        return transactionTemplate.execute(status -> userRepository.findById(id).orElseThrow());
    }
}
