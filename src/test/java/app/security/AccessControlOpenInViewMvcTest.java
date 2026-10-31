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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP access-control tests with open-in-view disabled and non-transactional requests,
 * matching production lazy-loading behavior.
 */
@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AccessControlOpenInViewMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

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

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    private String suffix;
    private User accountManager;
    private Customer managedCustomer;
    private Customer otherCustomer;
    private Shop shop;
    private TestPurchase managedPurchase;
    private TestPurchase otherPurchase;

    @BeforeEach
    void setUp() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        transactionTemplate.executeWithoutResult(status -> {
            testPurchaseRepository.deleteAll();
            userRepository.deleteAll();
            customerRepository.deleteAll();
            shopRepository.deleteAll();

            managedCustomer = customerRepository.save(Customer.builder()
                    .number("MC" + suffix)
                    .name("Managed " + suffix)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());

            otherCustomer = customerRepository.save(Customer.builder()
                    .number("OC" + suffix)
                    .name("Other " + suffix)
                    .country(Country.GERMANY)
                    .active(true)
                    .build());

            shop = shopRepository.save(Shop.builder()
                    .number("SH" + suffix)
                    .name("Shop " + suffix)
                    .country(Country.GERMANY)
                    .build());

            accountManager = userRepository.save(User.builder()
                    .username("am_" + suffix)
                    .email("am_" + suffix + "@access-mvc.test")
                    .password(passwordEncoder.encode("password"))
                    .role(Role.ACCOUNT_MANAGER)
                    .country(Country.GERMANY)
                    .active(true)
                    .managedCustomers(new HashSet<>(java.util.Set.of(managedCustomer)))
                    .build());

            managedPurchase = savePurchase("TP-M-" + suffix, managedCustomer);
            otherPurchase = savePurchase("TP-O-" + suffix, otherCustomer);
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            testPurchaseRepository.deleteAll();
            userRepository.deleteAll();
            customerRepository.deleteAll();
            shopRepository.deleteAll();
        });
    }

    @Test
    void accountManagerCanViewManagedPurchaseDetailWithAssignForm() throws Exception {
        mockMvc.perform(get("/testpurchases/view/" + managedPurchase.getId())
                        .with(user(userDetailsService.loadUserByUsername(accountManager.getUsername()))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("assign-mystery-shopper")));
    }

    @Test
    void accountManagerGetsForbiddenWhenViewingOtherClientsPurchase() throws Exception {
        var auth = user(userDetailsService.loadUserByUsername(accountManager.getUsername()));

        mockMvc.perform(get("/testpurchases/view/" + otherPurchase.getId()).with(auth))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/testpurchases/edit/" + otherPurchase.getId()).with(auth))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/testpurchases/edit/" + otherPurchase.getId())
                        .with(auth)
                        .with(csrf())
                        .param("id", otherPurchase.getId().toString())
                        .param("customerId", otherCustomer.getId().toString())
                        .param("shopId", shop.getId().toString())
                        .param("country", Country.GERMANY.name())
                        .param("category", TestPurchaseCategory.M.name())
                        .param("type", TestPurchaseType.RETURN_BACK_TO_SELLER.name())
                        .param("items[0].productName", "Item")
                        .param("items[0].quantity", "1")
                        .param("items[0].unitPrice", "10.0"))
                .andExpect(status().isForbidden());
    }

    @Test
    void mysteryShopperGetsForbiddenOnAddForm() throws Exception {
        User mysteryShopper = transactionTemplate.execute(status -> userRepository.save(User.builder()
                .username("ms_" + suffix)
                .email("ms_" + suffix + "@access-mvc.test")
                .password(passwordEncoder.encode("password"))
                .role(Role.MYSTERY_SHOPPER)
                .country(Country.GERMANY)
                .active(true)
                .managedCustomers(new HashSet<>())
                .build()));

        mockMvc.perform(get("/testpurchases/add")
                        .with(user(userDetailsService.loadUserByUsername(mysteryShopper.getUsername()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerGetsForbiddenWhenViewingAnotherCompanysPurchase() throws Exception {
        User customerUser = transactionTemplate.execute(status -> {
            User u = userRepository.save(User.builder()
                    .username("cust_" + suffix)
                    .email("cust_" + suffix + "@access-mvc.test")
                    .password(passwordEncoder.encode("password"))
                    .role(Role.CUSTOMER)
                    .country(Country.GERMANY)
                    .active(true)
                    .customer(managedCustomer)
                    .managedCustomers(new HashSet<>())
                    .build());
            return u;
        });

        mockMvc.perform(get("/testpurchases/view/" + otherPurchase.getId())
                        .with(user(userDetailsService.loadUserByUsername(customerUser.getUsername()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void salesManagerGetsForbiddenWhenViewingUnmanagedPurchase() throws Exception {
        User salesManager = transactionTemplate.execute(status -> userRepository.save(User.builder()
                .username("sm_" + suffix)
                .email("sm_" + suffix + "@access-mvc.test")
                .password(passwordEncoder.encode("password"))
                .role(Role.SALES_MANAGER)
                .country(Country.GERMANY)
                .active(true)
                .managedCustomers(new HashSet<>())
                .build()));

        managedCustomer.setSalesManager(salesManager);
        transactionTemplate.executeWithoutResult(status -> customerRepository.save(managedCustomer));

        mockMvc.perform(get("/testpurchases/view/" + otherPurchase.getId())
                        .with(user(userDetailsService.loadUserByUsername(salesManager.getUsername()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void registerWithoutTokenShowsInviteRequiredWithoutForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invite required")))
                .andExpect(content().string(not(containsString("<form"))));

        mockMvc.perform(get("/register").param("token", "not-a-valid-invite-token"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invite required")))
                .andExpect(content().string(not(containsString("<form"))));
    }

    private TestPurchase savePurchase(String number, Customer customer) {
        return testPurchaseRepository.save(TestPurchase.builder()
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
                .build());
    }
}
