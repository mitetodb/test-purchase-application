package app.security;

import app.TestPurchaseApplication;
import app.model.dto.TestPurchaseCreateDTO;
import app.service.TestPurchaseService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
import app.support.TestActiveUsers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
class CustomerDeactivationMvcIntegrationTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingCustomerClient pricingCustomerClient;

    @Autowired
    private TestPurchaseService testPurchaseService;

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
    private TransactionTemplate transactionTemplate;

    private final List<UUID> createdUserIds = new ArrayList<>();
    private final List<UUID> createdCustomerIds = new ArrayList<>();
    private final List<UUID> createdShopIds = new ArrayList<>();
    private final List<UUID> createdPurchaseIds = new ArrayList<>();

    private String suffix;
    private User admin;
    private Customer targetCustomer;
    private TestPurchase linkedPurchase;
    private Shop shop;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        TestActiveUsers.seedStandardUsers(userRepository, passwordEncoder);
        transactionTemplate.executeWithoutResult(status -> {
            admin = userRepository.save(User.builder()
                    .username("cust_admin_" + suffix)
                    .email("cust_admin_" + suffix + "@deact.test")
                    .password(passwordEncoder.encode("AdminPass1!"))
                    .role(Role.ADMIN)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
            createdUserIds.add(admin.getId());

            targetCustomer = customerRepository.save(Customer.builder()
                    .number("CD" + suffix)
                    .name("Customer " + suffix)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
            createdCustomerIds.add(targetCustomer.getId());

            shop = shopRepository.save(Shop.builder()
                    .number("SH" + suffix)
                    .name("Shop " + suffix)
                    .country(Country.GERMANY)
                    .build());
            createdShopIds.add(shop.getId());

            linkedPurchase = testPurchaseRepository.save(TestPurchase.builder()
                    .number("TP-" + suffix)
                    .customer(targetCustomer)
                    .shop(shop)
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
        TestActiveUsers.deleteStandardUsers(userRepository);
        transactionTemplate.executeWithoutResult(status -> {
            createdPurchaseIds.forEach(testPurchaseRepository::deleteById);
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
    void adminDeactivateKeepsCustomerAndLinkedPurchase() throws Exception {
        mockMvc.perform(post("/customers/deactivate/" + targetCustomer.getId())
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers"));

        Customer refreshed = customerRepository.findById(targetCustomer.getId()).orElseThrow();
        assertFalse(refreshed.isActive());
        assertNotNull(testPurchaseRepository.findById(linkedPurchase.getId()).orElse(null));
    }

    @Test
    void customerListHidesDeactivatedByDefault() throws Exception {
        transactionTemplate.executeWithoutResult(status -> {
            targetCustomer.setActive(false);
            customerRepository.save(targetCustomer);
        });

        mockMvc.perform(get("/customers")
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(targetCustomer.getName()))));
    }

    @Test
    void adminFilterShowsDeactivatedCustomers() throws Exception {
        transactionTemplate.executeWithoutResult(status -> {
            targetCustomer.setActive(false);
            customerRepository.save(targetCustomer);
        });

        mockMvc.perform(get("/customers")
                        .param("showDeactivated", "true")
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(targetCustomer.getName())));
    }

    @Test
    void adminCanReactivateCustomer() throws Exception {
        transactionTemplate.executeWithoutResult(status -> {
            targetCustomer.setActive(false);
            customerRepository.save(targetCustomer);
        });

        mockMvc.perform(post("/customers/reactivate/" + targetCustomer.getId())
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/customers?showDeactivated=true"));

        assertTrue(customerRepository.findById(targetCustomer.getId()).orElseThrow().isActive());
    }

    @Test
    void deleteEndpointNoLongerRemovesCustomer() throws Exception {
        mockMvc.perform(post("/customers/delete/" + targetCustomer.getId())
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        assertTrue(customerRepository.findById(targetCustomer.getId()).orElseThrow().isActive());
    }

    @Test
    void nonAdminDeactivateCustomerReturns403() throws Exception {
        User salesManager = transactionTemplate.execute(status -> {
            User sm = userRepository.save(User.builder()
                    .username("sm_cd_" + suffix)
                    .email("sm_cd_" + suffix + "@deact.test")
                    .password(passwordEncoder.encode("SmPass1!"))
                    .role(Role.SALES_MANAGER)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
            createdUserIds.add(sm.getId());
            return sm;
        });

        mockMvc.perform(post("/customers/deactivate/" + targetCustomer.getId())
                        .with(user(salesManager.getUsername()).roles("SALES_MANAGER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        assertTrue(customerRepository.findById(targetCustomer.getId()).orElseThrow().isActive());
    }

    @Test
    void cannotCreateTestPurchaseForDeactivatedCustomer() {
        transactionTemplate.executeWithoutResult(status -> {
            targetCustomer.setActive(false);
            customerRepository.save(targetCustomer);
        });

        TestPurchaseCreateDTO dto = new TestPurchaseCreateDTO();
        dto.setCustomerId(targetCustomer.getId());
        dto.setShopId(shop.getId());
        dto.setCountry(Country.GERMANY);
        dto.setCategory(TestPurchaseCategory.M);
        dto.setType(TestPurchaseType.RETURN_BACK_TO_SELLER);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        admin.getUsername(),
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        try {
            assertThrows(IllegalArgumentException.class, () -> testPurchaseService.create(dto));
        } finally {
            SecurityContextHolder.clearContext();
        }

        assertEquals(1, testPurchaseRepository.findAll().stream()
                .filter(tp -> tp.getCustomer().getId().equals(targetCustomer.getId()))
                .count());
    }
}
