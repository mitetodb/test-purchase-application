package app.security;

import app.TestPurchaseApplication;
import app.model.entity.Customer;
import app.model.entity.Invoice;
import app.model.entity.Shop;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.*;
import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
import app.service.CustomerService;
import app.service.InvoiceService;
import app.support.TestPurchaseTestDataCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Customer role IDOR: client user A must not reach client B's customer record, invoices, or statements.
 */
@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CustomerCrossClientIdorIntegrationTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private TestPurchaseRepository testPurchaseRepository;

    @Autowired
    private TestPurchaseTestDataCleaner testPurchaseTestDataCleaner;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Customer customerA;
    private Customer customerB;
    private User customerUserA;
    private User adminUser;
    private Shop shop;
    private YearMonth period;
    private Invoice invoiceForB;

    @BeforeEach
    void setUp() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        testPurchaseTestDataCleaner.resetPurchaseGraphAndBusinessFixtures(
                customerRepository, userRepository, shopRepository);

        app.model.dto.PriceCalculationResponseDTO pricing = new app.model.dto.PriceCalculationResponseDTO();
        pricing.setTestPurchaseFee(10.0);
        pricing.setPostageFee(5.0);
        pricing.setProductPrice(50.0);
        org.mockito.Mockito.when(pricingClient.calculatePrice(org.mockito.ArgumentMatchers.any())).thenReturn(pricing);

        customerA = saveCustomer("Client A", "CA100");
        customerB = saveCustomer("Client B", "CB200");

        customerUserA = saveUser("cust_a_idor", Role.CUSTOMER);
        customerUserA.setCustomer(customerA);
        userRepository.save(customerUserA);

        adminUser = saveUser("admin_idor_inv", Role.ADMIN);

        shop = new Shop();
        shop.setName("IDOR Shop");
        shop.setNumber("SHIDOR");
        shop.setCountry(Country.GERMANY);
        shop = shopRepository.save(shop);

        period = YearMonth.now().minusMonths(1);
        saveClosedPurchase(customerB, "TP-B-001", period.atDay(10).atStartOfDay());

        authenticate(adminUser, "ADMIN");
        invoiceForB = invoiceService.createDraftFromStatement(
                customerB.getId(), period.getYear(), period.getMonthValue(), null);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void customerUserCannotAccessOtherCustomersCustomerRecordPageOrApi() throws Exception {
        mockMvc.perform(get("/customers/edit/" + customerB.getId())
                        .with(user(customerUserA.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isForbidden());

        authenticate(customerUserA, "CUSTOMER");
        assertThrows(AccessDeniedException.class, () ->
                customerService.findByIdForCurrentUser(customerB.getId()));
    }

    @Test
    void customerUserCannotViewOtherCustomersInvoiceDetailPageOrApi() throws Exception {
        mockMvc.perform(get("/invoices/" + invoiceForB.getId())
                        .with(user(customerUserA.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isForbidden());

        authenticate(customerUserA, "CUSTOMER");
        assertThrows(AccessDeniedException.class, () ->
                invoiceService.findByIdForCurrentUser(invoiceForB.getId()));
    }

    @Test
    void customerUserCannotAccessOtherCustomersMonthlyStatementPageOrApi() throws Exception {
        mockMvc.perform(get("/invoices/statement")
                        .param("customerId", customerB.getId().toString())
                        .param("year", String.valueOf(period.getYear()))
                        .param("month", String.valueOf(period.getMonthValue()))
                        .with(user(customerUserA.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isForbidden());

        authenticate(customerUserA, "CUSTOMER");
        assertThrows(AccessDeniedException.class, () ->
                invoiceService.buildMonthlyStatement(
                        customerB.getId(), period.getYear(), period.getMonthValue()));
    }

    @Test
    void customerUserCanViewOwnCustomerRecordInvoicesAndStatement() throws Exception {
        saveClosedPurchase(customerA, "TP-A-001", period.atDay(8).atStartOfDay());
        authenticate(adminUser, "ADMIN");
        Invoice invoiceForA = invoiceService.createDraftFromStatement(
                customerA.getId(), period.getYear(), period.getMonthValue(), null);
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        authenticate(customerUserA, "CUSTOMER");
        assertDoesNotThrow(() -> customerService.findByIdForCurrentUser(customerA.getId()));
        assertDoesNotThrow(() -> invoiceService.findByIdForCurrentUser(invoiceForA.getId()));
        assertDoesNotThrow(() -> invoiceService.buildMonthlyStatement(
                customerA.getId(), period.getYear(), period.getMonthValue()));

        mockMvc.perform(get("/invoices/" + invoiceForA.getId())
                        .with(user(customerUserA.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/invoices/statement")
                        .param("customerId", customerA.getId().toString())
                        .param("year", String.valueOf(period.getYear()))
                        .param("month", String.valueOf(period.getMonthValue()))
                        .with(user(customerUserA.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isOk());
    }

    private Customer saveCustomer(String name, String number) {
        Customer c = Customer.builder()
                .name(name)
                .number(number)
                .country(Country.GERMANY)
                .category("GOLD")
                .active(true)
                .build();
        return customerRepository.save(c);
    }

    private User saveUser(String username, Role role) {
        User u = User.builder()
                .username(username)
                .email(username + "@test.com")
                .password(passwordEncoder.encode("password"))
                .role(role)
                .active(true)
                .country(Country.GERMANY)
                .managedCustomers(new java.util.HashSet<>())
                .build();
        return userRepository.save(u);
    }

    private void saveClosedPurchase(Customer customer, String number, LocalDateTime createdOn) {
        TestPurchase tp = new TestPurchase();
        tp.setCustomer(customer);
        tp.setShop(shop);
        tp.setNumber(number);
        tp.setCountry(Country.GERMANY);
        tp.setCategory(TestPurchaseCategory.S);
        tp.setType(TestPurchaseType.FORWARDING_TO_CLIENT);
        tp.setStatus(TestPurchaseStatus.CLOSED);
        tp.setActive(true);
        tp.setProductPrice(50.0);
        tp.setServiceFee(10.0);
        tp.setPostageFee(5.0);
        tp.setCreatedOn(createdOn);
        testPurchaseRepository.save(tp);
    }

    private void authenticate(User user, String role) {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        user.getUsername(), "x",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))));
    }
}
