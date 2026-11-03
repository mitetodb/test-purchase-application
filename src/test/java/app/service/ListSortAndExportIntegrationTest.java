package app.service;

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
import app.support.TestPurchaseTestDataCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ListSortAndExportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private TestPurchaseRepository testPurchaseRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TestPurchaseTestDataCleaner testPurchaseTestDataCleaner;

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    private User admin;
    private User accountManager;
    private Customer managedCustomer;
    private Customer otherCustomer;
    private Shop shop;

    @BeforeEach
    void setUp() {
        testPurchaseTestDataCleaner.resetPurchaseGraphAndBusinessFixtures(
                customerRepository, userRepository, shopRepository);

        admin = saveUser("sort_export_admin", Role.ADMIN);
        accountManager = saveUser("sort_export_am", Role.ACCOUNT_MANAGER);
        User salesManager = saveUser("sort_export_sm", Role.SALES_MANAGER);

        managedCustomer = saveCustomer("Managed Export Co", salesManager);
        otherCustomer = saveCustomer("Other Export Co", salesManager);
        accountManager.getManagedCustomers().add(managedCustomer);
        userRepository.save(accountManager);

        shop = saveShop("ExportShop");
    }

    @Test
    void allowedSortByNumberAscending() throws Exception {
        savePurchase("TP-Z-last", managedCustomer, LocalDate.of(2026, 3, 1));
        savePurchase("TP-A-first", managedCustomer, LocalDate.of(2026, 1, 1));

        mockMvc.perform(get("/testpurchases")
                        .param("sort", "number,asc")
                        .param("size", "10")
                        .with(user(userDetailsService.loadUserByUsername(admin.getUsername()))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("purchases", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(model().attribute("purchases", org.hamcrest.Matchers.contains(
                        org.hamcrest.Matchers.hasProperty("number", org.hamcrest.Matchers.equalTo("TP-A-first")),
                        org.hamcrest.Matchers.hasProperty("number", org.hamcrest.Matchers.equalTo("TP-Z-last"))
                )));
    }

    @Test
    void hiddenPasswordSortReturns200WithDefaultOrder() throws Exception {
        savePurchase("TP-old", managedCustomer, LocalDate.of(2026, 1, 1));
        savePurchase("TP-new", managedCustomer, LocalDate.of(2026, 6, 1));

        mockMvc.perform(get("/testpurchases")
                        .param("sort", "password,desc")
                        .with(user(userDetailsService.loadUserByUsername(admin.getUsername()))))
                .andExpect(status().isOk())
                .andExpect(model().attribute("purchases", org.hamcrest.Matchers.contains(
                        org.hamcrest.Matchers.hasProperty("number", org.hamcrest.Matchers.equalTo("TP-new")),
                        org.hamcrest.Matchers.hasProperty("number", org.hamcrest.Matchers.equalTo("TP-old"))
                )));
    }

    @Test
    void garbageSortPropertyReturns200WithDefaultOrder() throws Exception {
        savePurchase("TP-one", managedCustomer, LocalDate.of(2026, 1, 1));

        mockMvc.perform(get("/testpurchases")
                        .param("sort", "totallyInvalid,asc")
                        .with(user(userDetailsService.loadUserByUsername(admin.getUsername()))))
                .andExpect(status().isOk())
                .andExpect(view().name("testpurchases/testpurchases-list"));
    }

    @Test
    void csvExportIncludesAllMatchingRowsAcrossPages() throws Exception {
        for (int i = 1; i <= 5; i++) {
            savePurchase("TP-EXP-" + i, managedCustomer, LocalDate.of(2026, 1, i));
        }

        byte[] body = mockMvc.perform(get("/testpurchases/export.csv")
                        .with(user(userDetailsService.loadUserByUsername(admin.getUsername()))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("test-purchases.csv")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        String csv = new String(body, StandardCharsets.UTF_8);
        List<String> lines = csv.lines().filter(line -> !line.isBlank()).toList();
        assertThat(lines).hasSize(6);
        assertThat(csv).contains("TP-EXP-1", "TP-EXP-5");
    }

    @Test
    void accountManagerExportNeverIncludesForeignCustomers() throws Exception {
        savePurchase("TP-MINE", managedCustomer, LocalDate.of(2026, 1, 1));
        savePurchase("TP-FOREIGN", otherCustomer, LocalDate.of(2026, 1, 2));

        byte[] body = mockMvc.perform(get("/testpurchases/export.csv")
                        .with(user(userDetailsService.loadUserByUsername(accountManager.getUsername()))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        String csv = new String(body, StandardCharsets.UTF_8);
        assertThat(csv).contains("TP-MINE");
        assertThat(csv).doesNotContain("TP-FOREIGN");
    }

    private User saveUser(String username, Role role) {
        return userRepository.save(User.builder()
                .username(username)
                .email(username + "@sort-export.test")
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

    private void savePurchase(String number, Customer customer, LocalDate createdOn) {
        TestPurchase tp = TestPurchase.builder()
                .number(number)
                .customer(customer)
                .shop(shop)
                .country(Country.GERMANY)
                .category(TestPurchaseCategory.M)
                .type(TestPurchaseType.RETURN_BACK_TO_SELLER)
                .productPrice(10.0)
                .serviceFee(1.0)
                .postageFee(1.0)
                .active(true)
                .build();
        tp = testPurchaseRepository.saveAndFlush(tp);
        tp.setStatus(TestPurchaseStatus.INITIALISED);
        tp.setCreatedOn(createdOn.atStartOfDay());
        testPurchaseRepository.saveAndFlush(tp);
    }
}
