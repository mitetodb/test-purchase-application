package app.controller;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TestPurchasesListPageRenderTest {

    private static final DateTimeFormatter CREATED_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

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

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingCustomerClient pricingCustomerClient;

    @Test
    void adminCanRenderTestPurchasesList() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "adminTpList_" + suffix;
        User admin = saveUser(username, username + "@tp-list-render.test", Role.ADMIN);
        Shop shop = saveShop("ShopAdmin_" + suffix);
        Customer customer = saveCustomer("CustAdmin_" + suffix, null, null);
        TestPurchase purchase = saveTestPurchase("TP-ADM-" + suffix, customer, shop, null);

        expectPurchasesListRendered(
                performListAs(admin),
                purchase,
                null);
    }

    @Test
    void accountManagerCanRenderTestPurchasesList() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "accmgrTpList_" + suffix;
        User accountManager = saveUser(username, username + "@tp-list-render.test", Role.ACCOUNT_MANAGER);
        Shop shop = saveShop("ShopAccMgr_" + suffix);
        Customer customer = saveCustomer("CustAccMgr_" + suffix, null, null);
        accountManager.getManagedCustomers().add(customer);
        userRepository.save(accountManager);
        TestPurchase purchase = saveTestPurchase("TP-AM-" + suffix, customer, shop, null);

        expectPurchasesListRendered(
                performListAs(accountManager),
                purchase,
                null);
    }

    @Test
    void salesManagerCanRenderTestPurchasesList() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "salesmgrTpList_" + suffix;
        User salesManager = saveUser(username, username + "@tp-list-render.test", Role.SALES_MANAGER);
        String msUsername = "ms_secret_" + suffix;
        User mysteryShopper = saveUser(msUsername, msUsername + "@tp-list-render.test", Role.MYSTERY_SHOPPER);
        Shop shop = saveShop("ShopSales_" + suffix);
        Customer customer = saveCustomer("CustSales_" + suffix, null, salesManager);
        TestPurchase purchase = saveTestPurchase("TP-SM-" + suffix, customer, shop, mysteryShopper);

        expectPurchasesListRendered(
                performListAs(salesManager),
                purchase,
                msUsername);
    }

    @Test
    void customerCanRenderTestPurchasesList() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "customerTpList_" + suffix;
        User customerUser = saveUser(username, username + "@tp-list-render.test", Role.CUSTOMER);
        String msUsername = "ms_secret_" + suffix;
        User mysteryShopper = saveUser(msUsername, msUsername + "@tp-list-render.test", Role.MYSTERY_SHOPPER);
        Shop shop = saveShop("ShopCust_" + suffix);
        Customer customer = saveCustomer("CustUser_" + suffix, null, null);
        customerUser.setCustomer(customer);
        userRepository.save(customerUser);
        TestPurchase purchase = saveTestPurchase("TP-CU-" + suffix, customer, shop, mysteryShopper);

        expectPurchasesListRendered(
                performListAs(customerUser),
                purchase,
                msUsername);
    }

    @Test
    void mysteryShopperCanRenderTestPurchasesList() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "myshopperTpList_" + suffix;
        User mysteryShopper = saveUser(username, username + "@tp-list-render.test", Role.MYSTERY_SHOPPER);
        Shop shop = saveShop("ShopMs_" + suffix);
        Customer customer = saveCustomer("CustMs_" + suffix, null, null);
        TestPurchase purchase = saveTestPurchase("TP-MS-" + suffix, customer, shop, mysteryShopper);

        expectPurchasesListRendered(
                performListAs(mysteryShopper),
                purchase,
                null);
    }

    private ResultActions performListAs(User user) throws Exception {
        return mockMvc.perform(get("/testpurchases")
                .with(user(userDetailsService.loadUserByUsername(user.getUsername()))));
    }

    private void expectPurchasesListRendered(
            ResultActions actions,
            TestPurchase purchase,
            String htmlMustNotContain) throws Exception {
        testPurchaseRepository.flush();
        TestPurchase persisted = testPurchaseRepository.findById(purchase.getId()).orElseThrow();
        String formattedCreated = persisted.getCreatedOn().format(CREATED_DATE_FORMAT);

        actions
                .andExpect(status().isOk())
                .andExpect(view().name("testpurchases/testpurchases-list"))
                .andExpect(content().string(containsString(persisted.getNumber())))
                .andExpect(content().string(containsString(formattedCreated)))
                .andExpect(model().attribute("purchases", hasItem(allOf(
                        hasProperty("number", equalTo(persisted.getNumber())),
                        hasProperty("createdDate", equalTo(persisted.getCreatedOn()))
                ))));

        if (htmlMustNotContain != null) {
            actions.andExpect(content().string(not(containsString(htmlMustNotContain))));
        }
    }

    private User saveUser(String username, String email, Role role) {
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode("password"))
                .role(role)
                .active(true)
                .country(Country.GERMANY)
                .managedCustomers(new HashSet<>())
                .build();
        return userRepository.save(user);
    }

    private Customer saveCustomer(String name, User accountManager, User salesManager) {
        Customer customer = Customer.builder()
                .number(String.format("%05d", (int) (Math.random() * 100000)))
                .name(name)
                .country(Country.GERMANY)
                .active(true)
                .salesManager(salesManager)
                .build();
        return customerRepository.save(customer);
    }

    private Shop saveShop(String name) {
        Shop shop = Shop.builder()
                .number(String.format("%05d", (int) (Math.random() * 100000)))
                .name(name)
                .country(Country.GERMANY)
                .build();
        return shopRepository.save(shop);
    }

    private TestPurchase saveTestPurchase(String number, Customer customer, Shop shop, User mysteryShopper) {
        TestPurchase tp = TestPurchase.builder()
                .number(number)
                .customer(customer)
                .shop(shop)
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
}
