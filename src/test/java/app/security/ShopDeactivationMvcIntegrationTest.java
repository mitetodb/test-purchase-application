package app.security;

import app.TestPurchaseApplication;
import app.model.dto.TestPurchaseCreateDTO;
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
import app.service.TestPurchaseService;
import app.support.TestActiveUsers;
import app.support.TestPurchaseTestDataCleaner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ShopDeactivationMvcIntegrationTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingCustomerClient pricingCustomerClient;

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
    private TestPurchaseService testPurchaseService;

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
    private User admin;
    private Shop targetShop;
    private Customer customer;
    private TestPurchase linkedPurchase;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        TestActiveUsers.seedStandardUsers(userRepository, passwordEncoder);
        transactionTemplate.executeWithoutResult(status -> {
            admin = userRepository.save(User.builder()
                    .username("shop_admin_" + suffix)
                    .email("shop_admin_" + suffix + "@deact.test")
                    .password(passwordEncoder.encode("AdminPass1!"))
                    .role(Role.ADMIN)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
            createdUserIds.add(admin.getId());

            customer = customerRepository.save(Customer.builder()
                    .number("CS" + suffix)
                    .name("Customer " + suffix)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
            createdCustomerIds.add(customer.getId());

            targetShop = shopRepository.save(Shop.builder()
                    .number("SD" + suffix)
                    .name("Shop " + suffix)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());
            createdShopIds.add(targetShop.getId());

            linkedPurchase = testPurchaseRepository.save(TestPurchase.builder()
                    .number("TP-SD-" + suffix)
                    .customer(customer)
                    .shop(targetShop)
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
    void adminDeactivateKeepsShopAndLinkedPurchase() throws Exception {
        mockMvc.perform(post("/shops/deactivate/" + targetShop.getId())
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shops"));

        Shop refreshed = shopRepository.findById(targetShop.getId()).orElseThrow();
        assertFalse(refreshed.isActive());
        assertNotNull(testPurchaseRepository.findById(linkedPurchase.getId()).orElse(null));
    }

    @Test
    void shopListHidesDeactivatedByDefault() throws Exception {
        transactionTemplate.executeWithoutResult(status -> {
            targetShop.setActive(false);
            shopRepository.save(targetShop);
        });

        mockMvc.perform(get("/shops")
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(not(containsString(targetShop.getName()))));
    }

    @Test
    void adminFilterShowsDeactivatedShops() throws Exception {
        transactionTemplate.executeWithoutResult(status -> {
            targetShop.setActive(false);
            shopRepository.save(targetShop);
        });

        mockMvc.perform(get("/shops")
                        .param("showDeactivated", "true")
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(containsString(targetShop.getName())));
    }

    @Test
    void deleteEndpointNoLongerRemovesShop() throws Exception {
        mockMvc.perform(post("/shops/delete/" + targetShop.getId())
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNotFound());

        assertTrue(shopRepository.findById(targetShop.getId()).orElseThrow().isActive());
    }

    @Test
    void cannotCreateTestPurchaseForDeactivatedShop() {
        transactionTemplate.executeWithoutResult(status -> {
            targetShop.setActive(false);
            shopRepository.save(targetShop);
        });

        TestPurchaseCreateDTO dto = new TestPurchaseCreateDTO();
        dto.setCustomerId(customer.getId());
        dto.setShopId(targetShop.getId());
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
                .filter(tp -> tp.getShop().getId().equals(targetShop.getId()))
                .count());
    }
}
