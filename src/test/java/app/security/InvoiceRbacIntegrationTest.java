package app.security;

import app.TestPurchaseApplication;
import app.model.entity.Customer;
import app.model.entity.Shop;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.*;
import app.repository.CustomerRepository;
import app.repository.InvoiceRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class InvoiceRbacIntegrationTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoiceRepository invoiceRepository;

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

    private Customer managedCustomer;
    private Customer otherCustomer;
    private User accountManager;
    private User salesManager;
    private User customerUser;
    private User mysteryShopper;
    private Shop shop;
    private YearMonth period;

    @BeforeEach
    void setUp() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        testPurchaseTestDataCleaner.resetPurchaseGraphAndBusinessFixtures(
                customerRepository, userRepository, shopRepository);

        app.model.dto.PriceCalculationResponseDTO pricing = new app.model.dto.PriceCalculationResponseDTO();
        pricing.setTestPurchaseFee(15.0);
        pricing.setPostageFee(5.0);
        pricing.setProductPrice(100.0);
        org.mockito.Mockito.when(pricingClient.calculatePrice(org.mockito.ArgumentMatchers.any())).thenReturn(pricing);

        managedCustomer = saveCustomer("Managed Co", "C9001");
        otherCustomer = saveCustomer("Other Co", "C9002");

        accountManager = saveUser("am_inv", Role.ACCOUNT_MANAGER);
        accountManager.getManagedCustomers().add(managedCustomer);
        userRepository.save(accountManager);

        salesManager = saveUser("sm_inv", Role.SALES_MANAGER);
        managedCustomer.setSalesManager(salesManager);
        customerRepository.save(managedCustomer);

        customerUser = saveUser("cust_inv", Role.CUSTOMER);
        customerUser.setCustomer(managedCustomer);
        userRepository.save(customerUser);

        mysteryShopper = saveUser("ms_inv", Role.MYSTERY_SHOPPER);

        shop = new Shop();
        shop.setName("Shop Inv");
        shop.setNumber("S9001");
        shop.setCountry(Country.GERMANY);
        shop = shopRepository.save(shop);

        period = YearMonth.now().minusMonths(1);
        saveClosedPurchase(managedCustomer, "TP-INV-1", period.atDay(5).atStartOfDay());
        saveClosedPurchase(otherCustomer, "TP-INV-2", period.atDay(6).atStartOfDay());
    }

    @Test
    void accountManagerCanCreateIssueAndMarkPaidForManagedCustomer() {
        authenticate(accountManager, "ACCOUNT_MANAGER");
        var invoice = invoiceService.createDraftFromStatement(
                managedCustomer.getId(), period.getYear(), period.getMonthValue(), null);
        assertEquals(InvoiceStatus.DRAFT, invoice.getStatus());
        assertTrue(invoice.getNumber().startsWith("INV-"));

        invoice = invoiceService.issue(invoice.getId());
        assertEquals(InvoiceStatus.ISSUED, invoice.getStatus());
        assertNotNull(invoice.getIssuedAt());

        invoice = invoiceService.markPaid(invoice.getId());
        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertNotNull(invoice.getPaidAt());
    }

    @Test
    void accountManagerCannotManageInvoicesForUnmanagedCustomer() {
        authenticate(accountManager, "ACCOUNT_MANAGER");
        assertThrows(AccessDeniedException.class, () ->
                invoiceService.createDraftFromStatement(
                        otherCustomer.getId(), period.getYear(), period.getMonthValue(), null));
    }

    @Test
    void salesManagerCanViewButNotCreateInvoice() throws Exception {
        var invoice = loginAsAdminCreateDraft();

        mockMvc.perform(get("/invoices/" + invoice.getId())
                        .with(user(salesManager.getUsername()).roles("SALES_MANAGER")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/invoices/create-from-statement")
                        .with(user(salesManager.getUsername()).roles("SALES_MANAGER"))
                        .with(csrf())
                        .param("customerId", managedCustomer.getId().toString())
                        .param("year", String.valueOf(period.getYear()))
                        .param("month", String.valueOf(period.getMonthValue())))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCanViewOwnInvoiceAndPdfButNotIssue() throws Exception {
        var invoice = loginAsAdminCreateDraft();
        invoice = invoiceService.issue(invoice.getId());

        mockMvc.perform(get("/invoices/" + invoice.getId())
                        .with(user(customerUser.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/invoices/" + invoice.getId() + "/pdf")
                        .with(user(customerUser.getUsername()).roles("CUSTOMER")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/invoices/" + invoice.getId() + "/issue")
                        .with(user(customerUser.getUsername()).roles("CUSTOMER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void mysteryShopperCannotAccessInvoices() throws Exception {
        mockMvc.perform(get("/invoices")
                        .with(user(mysteryShopper.getUsername()).roles("MYSTERY_SHOPPER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelIsSoftStatusNotDelete() {
        authenticate(accountManager, "ACCOUNT_MANAGER");
        var invoice = loginAsAdminCreateDraft();
        invoice = invoiceService.issue(invoice.getId());
        UUID id = invoice.getId();
        invoiceService.cancel(id);
        assertTrue(invoiceRepository.findById(id).isPresent());
        assertEquals(InvoiceStatus.CANCELLED, invoiceRepository.findById(id).orElseThrow().getStatus());
    }

    private app.model.entity.Invoice loginAsAdminCreateDraft() {
        User admin = saveUser("admin_inv_" + System.nanoTime(), Role.ADMIN);
        authenticate(admin, "ADMIN");
        return invoiceService.createDraftFromStatement(
                managedCustomer.getId(), period.getYear(), period.getMonthValue(), "demo");
    }

    private void authenticate(User user, String role) {
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        user.getUsername(), "x",
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role))));
    }

    private Customer saveCustomer(String name, String number) {
        Customer c = Customer.builder()
                .name(name)
                .number(number)
                .country(Country.GERMANY)
                .category("GOLD")
                .active(true)
                .baseServiceFee(25.0)
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
        tp.setProductPrice(100.0);
        tp.setServiceFee(15.0);
        tp.setPostageFee(5.0);
        tp.setCreatedOn(createdOn);
        testPurchaseRepository.save(tp);
    }
}
