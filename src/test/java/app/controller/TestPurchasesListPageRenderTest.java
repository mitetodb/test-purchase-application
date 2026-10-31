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
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TestPurchasesListPageRenderTest {

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
        saveTestPurchase("TP-ADM-" + suffix, customer, shop, null);

        mockMvc.perform(get("/testpurchases").with(user(userDetailsService.loadUserByUsername(admin.getUsername()))))
                .andExpect(status().isOk())
                .andExpect(view().name("testpurchases/testpurchases-list"));
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
        saveTestPurchase("TP-AM-" + suffix, customer, shop, null);

        mockMvc.perform(get("/testpurchases").with(user(userDetailsService.loadUserByUsername(accountManager.getUsername()))))
                .andExpect(status().isOk())
                .andExpect(view().name("testpurchases/testpurchases-list"));
    }

    @Test
    void salesManagerCanRenderTestPurchasesList() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "salesmgrTpList_" + suffix;
        User salesManager = saveUser(username, username + "@tp-list-render.test", Role.SALES_MANAGER);
        Shop shop = saveShop("ShopSales_" + suffix);
        Customer customer = saveCustomer("CustSales_" + suffix, null, salesManager);
        saveTestPurchase("TP-SM-" + suffix, customer, shop, null);

        mockMvc.perform(get("/testpurchases").with(user(userDetailsService.loadUserByUsername(salesManager.getUsername()))))
                .andExpect(status().isOk())
                .andExpect(view().name("testpurchases/testpurchases-list"));
    }

    @Test
    void customerCanRenderTestPurchasesList() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "customerTpList_" + suffix;
        User customerUser = saveUser(username, username + "@tp-list-render.test", Role.CUSTOMER);
        Shop shop = saveShop("ShopCust_" + suffix);
        Customer customer = saveCustomer("CustUser_" + suffix, null, null);
        customerUser.setCustomer(customer);
        userRepository.save(customerUser);
        saveTestPurchase("TP-CU-" + suffix, customer, shop, null);

        mockMvc.perform(get("/testpurchases").with(user(userDetailsService.loadUserByUsername(customerUser.getUsername()))))
                .andExpect(status().isOk())
                .andExpect(view().name("testpurchases/testpurchases-list"));
    }

    @Test
    void mysteryShopperCanRenderTestPurchasesList() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "myshopperTpList_" + suffix;
        User mysteryShopper = saveUser(username, username + "@tp-list-render.test", Role.MYSTERY_SHOPPER);
        Shop shop = saveShop("ShopMs_" + suffix);
        Customer customer = saveCustomer("CustMs_" + suffix, null, null);
        saveTestPurchase("TP-MS-" + suffix, customer, shop, mysteryShopper);

        mockMvc.perform(get("/testpurchases").with(user(userDetailsService.loadUserByUsername(mysteryShopper.getUsername()))))
                .andExpect(status().isOk())
                .andExpect(view().name("testpurchases/testpurchases-list"));
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
