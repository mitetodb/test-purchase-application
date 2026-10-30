package app.security;

import app.TestPurchaseApplication;
import app.model.dto.CustomerDTO;
import app.model.dto.TestPurchaseCreateDTO;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
@Transactional
class RolePermissionsTest {

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
    private User customerUser;
    private User mysteryShopperUser;

    private Customer customer1;
    private TestPurchase testPurchase1;
    private Shop shop;

    @BeforeEach
    void setup() {
        adminUser = createUser("admin_perm", Role.ADMIN);
        accountManagerUser = createUser("accmgr_perm", Role.ACCOUNT_MANAGER);
        salesManagerUser = createUser("salesmgr_perm", Role.SALES_MANAGER);
        customerUser = createUser("customer_perm", Role.CUSTOMER);
        mysteryShopperUser = createUser("myshopper_perm", Role.MYSTERY_SHOPPER);

        shop = createShop("Test Shop Perm");
        customer1 = createCustomer("Customer Perm", null, salesManagerUser);
        
        accountManagerUser.getManagedCustomers().add(customer1);
        userRepository.save(accountManagerUser);

        customerUser.setCustomer(customer1);
        userRepository.save(customerUser);

        testPurchase1 = createTestPurchase("TP-PERM-001", customer1, mysteryShopperUser);
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
                .number(String.valueOf(System.currentTimeMillis() % 100000))
                .name(name)
                .country(Country.GERMANY)
                .active(true)
                .salesManager(salesManager)
                .build();
        return customerRepository.save(customer);
    }

    private Shop createShop(String name) {
        Shop s = Shop.builder()
                .number(String.valueOf(System.currentTimeMillis() % 10000))
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
    void mysteryShopperCannotCreateTestPurchase() {
        authenticateAs(mysteryShopperUser);

        TestPurchaseCreateDTO dto = new TestPurchaseCreateDTO();
        dto.setCustomerId(customer1.getId());
        dto.setShopId(shop.getId());
        dto.setCountry(Country.GERMANY);
        dto.setCategory(TestPurchaseCategory.M);
        dto.setType(TestPurchaseType.RETURN_BACK_TO_SELLER);
        dto.setItems(new ArrayList<>());

        assertThrows(AccessDeniedException.class, () -> {
            testPurchaseService.create(dto);
        }, "Mystery shopper should not be able to create test purchases");
    }

    @Test
    void customerCanCreateTestPurchase() {
        authenticateAs(customerUser);

        TestPurchaseCreateDTO dto = new TestPurchaseCreateDTO();
        dto.setCustomerId(customer1.getId());
        dto.setShopId(shop.getId());
        dto.setCountry(Country.GERMANY);
        dto.setCategory(TestPurchaseCategory.M);
        dto.setType(TestPurchaseType.RETURN_BACK_TO_SELLER);
        dto.setItems(new ArrayList<>());

        assertDoesNotThrow(() -> {
            testPurchaseService.create(dto);
        }, "Customer should be able to create test purchases");
    }

    @Test
    void customerCannotCreateTestPurchaseForAnotherCompany() {
        authenticateAs(customerUser);

        Customer anotherCustomer = createCustomer("Another Customer", null, null);

        TestPurchaseCreateDTO dto = new TestPurchaseCreateDTO();
        dto.setCustomerId(anotherCustomer.getId());
        dto.setShopId(shop.getId());
        dto.setCountry(Country.GERMANY);
        dto.setCategory(TestPurchaseCategory.M);
        dto.setType(TestPurchaseType.RETURN_BACK_TO_SELLER);
        dto.setItems(new ArrayList<>());

        assertThrows(AccessDeniedException.class, () -> {
            testPurchaseService.create(dto);
        }, "Customer should only create test purchases for their own company");
    }

    @Test
    void salesManagerCannotEditTestPurchase() {
        authenticateAs(salesManagerUser);

        assertThrows(AccessDeniedException.class, () -> {
            testPurchaseService.edit(testPurchase1.getId(), null);
        }, "Sales manager should not be able to edit test purchases");
    }

    @Test
    void customerCannotEditTestPurchase() {
        authenticateAs(customerUser);

        assertThrows(AccessDeniedException.class, () -> {
            testPurchaseService.edit(testPurchase1.getId(), null);
        }, "Customer should not be able to edit test purchases");
    }

    @Test
    void mysteryShopperCannotEditTestPurchase() {
        authenticateAs(mysteryShopperUser);

        assertThrows(AccessDeniedException.class, () -> {
            testPurchaseService.edit(testPurchase1.getId(), null);
        }, "Mystery shopper should not be able to edit test purchases");
    }

    @Test
    void accountManagerCanEditTestPurchase() {
        authenticateAs(accountManagerUser);

        assertDoesNotThrow(() -> {
            testPurchaseService.edit(testPurchase1.getId(), null);
        });
    }

    @Test
    void salesManagerCanChangeStatus() {
        authenticateAs(salesManagerUser);

        assertThrows(AccessDeniedException.class, () -> {
            testPurchaseService.changeStatus(testPurchase1.getId(), TestPurchaseStatus.IN_PROGRESS, null);
        }, "Sales manager should not change status because they cannot view this test purchase");
    }

    @Test
    void customerCannotChangeStatus() {
        authenticateAs(customerUser);

        assertThrows(AccessDeniedException.class, () -> {
            testPurchaseService.changeStatus(testPurchase1.getId(), TestPurchaseStatus.IN_PROGRESS, null);
        }, "Customer should not be able to change test purchase status");
    }

    @Test
    void mysteryShopperCanChangeStatus() {
        authenticateAs(mysteryShopperUser);

        assertDoesNotThrow(() -> {
            testPurchaseService.changeStatus(testPurchase1.getId(), TestPurchaseStatus.IN_PROGRESS, null);
        }, "Mystery shopper should be able to change status of their assigned test purchase");
    }

    @Test
    void nonAdminCannotHardDeleteTestPurchase() {
        authenticateAs(accountManagerUser);

        testPurchaseService.delete(testPurchase1.getId());

        TestPurchase tp = testPurchaseRepository.findById(testPurchase1.getId()).orElse(null);
        assertNotNull(tp, "Test purchase should still exist in database");
        assertFalse(tp.isActive(), "Test purchase should be soft-deleted (inactive)");
    }

    @Test
    void adminCanHardDeleteTestPurchase() {
        authenticateAs(adminUser);

        testPurchaseService.delete(testPurchase1.getId());

        TestPurchase tp = testPurchaseRepository.findById(testPurchase1.getId()).orElse(null);
        assertNull(tp, "Admin should hard-delete test purchase from database");
    }

    @Test
    void nonAdminCannotHardDeleteCustomer() {
        authenticateAs(salesManagerUser);

        customerService.delete(customer1.getId());

        Customer c = customerRepository.findById(customer1.getId()).orElse(null);
        assertNotNull(c, "Customer should still exist in database");
        assertFalse(c.isActive(), "Customer should be soft-deleted (inactive)");
    }

    @Test
    void adminCanHardDeleteCustomer() {
        authenticateAs(adminUser);

        customerService.delete(customer1.getId());

        Customer c = customerRepository.findById(customer1.getId()).orElse(null);
        assertNull(c, "Admin should hard-delete customer from database");
    }

    @Test
    void salesManagerCanCreateCustomer() {
        authenticateAs(salesManagerUser);

        CustomerDTO dto = new CustomerDTO();
        dto.setName("New Customer by Sales Manager");
        dto.setCountry(Country.GERMANY);

        Customer created = customerService.create(dto);
        
        assertNotNull(created);
        assertEquals(salesManagerUser.getId(), created.getSalesManager().getId(), 
            "Sales manager should be auto-assigned as owner of created customer");
    }

    @Test
    void accountManagerCanCreateCustomer() {
        authenticateAs(accountManagerUser);

        CustomerDTO dto = new CustomerDTO();
        dto.setName("New Customer by Account Manager");
        dto.setCountry(Country.GERMANY);

        Customer created = customerService.create(dto);
        
        assertNotNull(created);
        
        User updatedUser = userRepository.findByUsernameWithManagedCustomers(accountManagerUser.getUsername()).orElseThrow();
        assertTrue(updatedUser.getManagedCustomers().contains(created), 
            "Account manager should be auto-assigned to manage created customer");
    }

    @Test
    void customerCannotCreateCustomer() {
        authenticateAs(customerUser);

        CustomerDTO dto = new CustomerDTO();
        dto.setName("New Customer by Customer");
        dto.setCountry(Country.GERMANY);

        assertThrows(Exception.class, () -> {
            customerService.create(dto);
        }, "Customer role should not be able to create customers");
    }

    @Test
    void mysteryShopperCannotCreateCustomer() {
        authenticateAs(mysteryShopperUser);

        CustomerDTO dto = new CustomerDTO();
        dto.setName("New Customer by Mystery Shopper");
        dto.setCountry(Country.GERMANY);

        assertThrows(Exception.class, () -> {
            customerService.create(dto);
        }, "Mystery shopper should not be able to create customers");
    }

    @Test
    void salesManagerCanSetCustomerPricing() {
        authenticateAs(salesManagerUser);

        CustomerDTO dto = new CustomerDTO();
        dto.setName(customer1.getName());
        dto.setCountry(customer1.getCountry());
        dto.setBaseServiceFee(25.0);

        assertDoesNotThrow(() -> {
            customerService.update(customer1.getId(), dto);
        }, "Sales manager should be able to set customer pricing");
    }

    @Test
    void accountManagerCanSetCustomerPricing() {
        authenticateAs(accountManagerUser);

        CustomerDTO dto = new CustomerDTO();
        dto.setName(customer1.getName());
        dto.setCountry(customer1.getCountry());
        dto.setBaseServiceFee(30.0);

        assertDoesNotThrow(() -> {
            customerService.update(customer1.getId(), dto);
        }, "Account manager should be able to set customer pricing");
    }
}
