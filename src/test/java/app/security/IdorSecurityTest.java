package app.security;

import app.TestPurchaseApplication;
import app.model.dto.TestPurchaseViewDTO;
import app.model.entity.Customer;
import app.model.entity.Shop;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.*;
import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
import app.service.CustomerService;
import app.service.TestPurchaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
@Transactional
class IdorSecurityTest {

    @Autowired
    private TestPurchaseService testPurchaseService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private TestPurchaseRepository testPurchaseRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User accountManagerUser;
    private User salesManagerUser;
    private User customerUser1;
    private User customerUser2;
    private User mysteryShopperUser1;
    private User mysteryShopperUser2;

    private Customer customer1;
    private Customer customer2;
    private Customer customer3;

    private TestPurchase testPurchase1;
    private TestPurchase testPurchase2;
    private TestPurchase testPurchase3;

    private Shop shop;

    @BeforeEach
    void setup() {
        adminUser = createUser("admin_idor", Role.ADMIN);
        accountManagerUser = createUser("accmgr_idor", Role.ACCOUNT_MANAGER);
        salesManagerUser = createUser("salesmgr_idor", Role.SALES_MANAGER);
        customerUser1 = createUser("customer1_idor", Role.CUSTOMER);
        customerUser2 = createUser("customer2_idor", Role.CUSTOMER);
        mysteryShopperUser1 = createUser("myshopper1_idor", Role.MYSTERY_SHOPPER);
        mysteryShopperUser2 = createUser("myshopper2_idor", Role.MYSTERY_SHOPPER);

        shop = createShop("Test Shop IDOR");

        customer1 = createCustomer("Customer 1 IDOR", accountManagerUser, null);
        customer2 = createCustomer("Customer 2 IDOR", null, salesManagerUser);
        customer3 = createCustomer("Customer 3 IDOR", null, null);

        accountManagerUser.getManagedCustomers().add(customer1);
        userRepository.save(accountManagerUser);

        customerUser1.setCustomer(customer1);
        userRepository.save(customerUser1);

        customerUser2.setCustomer(customer2);
        userRepository.save(customerUser2);

        testPurchase1 = createTestPurchase("TP-IDOR-001", customer1, mysteryShopperUser1);
        testPurchase2 = createTestPurchase("TP-IDOR-002", customer2, mysteryShopperUser2);
        testPurchase3 = createTestPurchase("TP-IDOR-003", customer3, null);
    }

    private User createUser(String username, Role role) {
        User user = User.builder()
                .username(username)
                .email(username + "@test.com")
                .password(passwordEncoder.encode("password"))
                .role(role)
                .active(true)
                .country(Country.GERMANY)
                .managedCustomers(new HashSet<>())
                .build();
        return userRepository.save(user);
    }

    private Customer createCustomer(String name, User accountManager, User salesManager) {
        Customer customer = Customer.builder()
                .number(String.format("%05d", (int)(Math.random() * 100000)))
                .name(name)
                .country(Country.GERMANY)
                .active(true)
                .salesManager(salesManager)
                .build();
        return customerRepository.save(customer);
    }

    private Shop createShop(String name) {
        Shop s = Shop.builder()
                .number(String.format("%05d", (int)(Math.random() * 100000)))
                .name(name)
                .country(Country.GERMANY)
                .build();
        return shopRepository.save(s);
    }

    private TestPurchase createTestPurchase(String number, Customer customer, User mysteryShopper) {
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
                .active(true)
                .build();
        return testPurchaseRepository.save(tp);
    }

    private void authenticateAs(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void mysteryShopperCannotViewOtherMysteryShoppersTestPurchase() {
        authenticateAs(mysteryShopperUser1);

        assertThrows(AccessDeniedException.class, () -> {
            testPurchaseService.findByIdForCurrentUser(testPurchase2.getId());
        }, "Mystery shopper should not access another mystery shopper's test purchase");
    }

    @Test
    void mysteryShopperCanViewTheirOwnTestPurchase() {
        authenticateAs(mysteryShopperUser1);

        TestPurchaseViewDTO result = testPurchaseService.findByIdForCurrentUser(testPurchase1.getId());
        assertNotNull(result);
        assertEquals(testPurchase1.getId(), result.getId());
    }

    @Test
    void mysteryShopperCannotViewUnassignedTestPurchase() {
        authenticateAs(mysteryShopperUser1);

        assertThrows(AccessDeniedException.class, () -> {
            testPurchaseService.findByIdForCurrentUser(testPurchase3.getId());
        }, "Mystery shopper should not access unassigned test purchase");
    }

    @Test
    void customerUserCannotViewOtherCustomersTestPurchase() {
        authenticateAs(customerUser1);

        assertThrows(AccessDeniedException.class, () -> {
            testPurchaseService.findByIdForCurrentUser(testPurchase2.getId());
        }, "Customer user should not access another customer's test purchase");
    }

    @Test
    void customerUserCanViewTheirOwnCompanyTestPurchase() {
        authenticateAs(customerUser1);

        TestPurchaseViewDTO result = testPurchaseService.findByIdForCurrentUser(testPurchase1.getId());
        assertNotNull(result);
        assertEquals(testPurchase1.getId(), result.getId());
    }

    @Test
    void salesManagerCannotViewTestPurchaseFromNonOwnedCustomer() {
        authenticateAs(salesManagerUser);

        assertThrows(AccessDeniedException.class, () -> {
            testPurchaseService.findByIdForCurrentUser(testPurchase1.getId());
        }, "Sales manager should not access test purchase from non-owned customer");
    }

    @Test
    void salesManagerCanViewTestPurchaseFromOwnedCustomer() {
        authenticateAs(salesManagerUser);

        TestPurchaseViewDTO result = testPurchaseService.findByIdForCurrentUser(testPurchase2.getId());
        assertNotNull(result);
        assertEquals(testPurchase2.getId(), result.getId());
    }

    @Test
    void accountManagerCannotViewTestPurchaseFromNonManagedCustomer() {
        authenticateAs(accountManagerUser);

        assertThrows(AccessDeniedException.class, () -> {
            testPurchaseService.findByIdForCurrentUser(testPurchase2.getId());
        }, "Account manager should not access test purchase from non-managed customer");
    }

    @Test
    void accountManagerCanViewTestPurchaseFromManagedCustomer() {
        authenticateAs(accountManagerUser);

        TestPurchaseViewDTO result = testPurchaseService.findByIdForCurrentUser(testPurchase1.getId());
        assertNotNull(result);
        assertEquals(testPurchase1.getId(), result.getId());
    }

    @Test
    void salesManagerCannotViewNonOwnedCustomer() {
        authenticateAs(salesManagerUser);

        assertThrows(AccessDeniedException.class, () -> {
            customerService.findByIdForCurrentUser(customer1.getId());
        }, "Sales manager should not access non-owned customer");
    }

    @Test
    void salesManagerCanViewOwnedCustomer() {
        authenticateAs(salesManagerUser);

        Customer result = customerService.findByIdForCurrentUser(customer2.getId());
        assertNotNull(result);
        assertEquals(customer2.getId(), result.getId());
    }

    @Test
    void accountManagerCannotViewNonManagedCustomer() {
        authenticateAs(accountManagerUser);

        assertThrows(AccessDeniedException.class, () -> {
            customerService.findByIdForCurrentUser(customer2.getId());
        }, "Account manager should not access non-managed customer");
    }

    @Test
    void accountManagerCanViewManagedCustomer() {
        authenticateAs(accountManagerUser);

        Customer result = customerService.findByIdForCurrentUser(customer1.getId());
        assertNotNull(result);
        assertEquals(customer1.getId(), result.getId());
    }

    @Test
    void customerRoleUserCannotAccessAnyCustomerData() {
        authenticateAs(customerUser1);

        assertThrows(AccessDeniedException.class, () -> {
            customerService.findByIdForCurrentUser(customer1.getId());
        }, "Customer role should not access customer management data");
    }

    @Test
    void mysteryShopperCannotAccessAnyCustomerData() {
        authenticateAs(mysteryShopperUser1);

        assertThrows(AccessDeniedException.class, () -> {
            customerService.findByIdForCurrentUser(customer1.getId());
        }, "Mystery shopper should not access customer management data");
    }

    @Test
    void salesManagerAndCustomerShouldNotSeeMysteryShopperName() {
        authenticateAs(salesManagerUser);

        TestPurchaseViewDTO result = testPurchaseService.findByIdForCurrentUser(testPurchase2.getId());
        assertNull(result.getMysteryShopper(), 
            "Sales manager should not see mystery shopper name");

        authenticateAs(customerUser1);
        
        TestPurchaseViewDTO result2 = testPurchaseService.findByIdForCurrentUser(testPurchase1.getId());
        assertNull(result2.getMysteryShopper(), 
            "Customer should not see mystery shopper name");
    }

    @Test
    void adminAndAccountManagerShouldSeeMysteryShopperName() {
        authenticateAs(adminUser);

        TestPurchaseViewDTO result = testPurchaseService.findByIdForCurrentUser(testPurchase1.getId());
        assertNotNull(result.getMysteryShopper(), 
            "Admin should see mystery shopper name");

        authenticateAs(accountManagerUser);

        TestPurchaseViewDTO result2 = testPurchaseService.findByIdForCurrentUser(testPurchase1.getId());
        assertNotNull(result2.getMysteryShopper(), 
            "Account manager should see mystery shopper name");
    }

    @Test
    void inactiveTestPurchaseShouldNotBeAccessible() {
        testPurchase1.setActive(false);
        testPurchaseRepository.save(testPurchase1);

        authenticateAs(mysteryShopperUser1);

        assertThrows(EntityNotFoundException.class, () -> {
            testPurchaseService.findByIdForCurrentUser(testPurchase1.getId());
        }, "Inactive test purchase should return 404 not 403");
    }

    @Test
    void inactiveCustomerShouldNotBeAccessible() {
        customer1.setActive(false);
        customerRepository.save(customer1);

        authenticateAs(accountManagerUser);

        assertThrows(IllegalArgumentException.class, () -> {
            customerService.findByIdForCurrentUser(customer1.getId());
        }, "Inactive customer should not be accessible");
    }
}
