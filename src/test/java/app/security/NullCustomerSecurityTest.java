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
import app.service.TestPurchaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that NULL customer_id or sales_manager_id fails closed (sees nothing).
 */
@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
@Transactional
class NullCustomerSecurityTest {

    @Autowired
    private TestPurchaseService testPurchaseService;

    @Autowired
    private TestPurchaseRepository testPurchaseRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    private User customerUserWithNullCustomer;
    private User salesManagerWithNullManaged;
    private TestPurchase orphanPurchase;

    @BeforeEach
    void setUp() {
        testPurchaseRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();
        shopRepository.deleteAll();

        // Create a customer user with NO customer link
        customerUserWithNullCustomer = new User();
        customerUserWithNullCustomer.setUsername("customer_null");
        customerUserWithNullCustomer.setPassword("password");
        customerUserWithNullCustomer.setEmail("null@test.com");
        customerUserWithNullCustomer.setRole(Role.CUSTOMER);
        customerUserWithNullCustomer.setActive(true);
        customerUserWithNullCustomer.setCountry(Country.GERMANY);
        customerUserWithNullCustomer.setCustomer(null);  // NULL customer
        customerUserWithNullCustomer = userRepository.save(customerUserWithNullCustomer);

        // Create a sales manager
        salesManagerWithNullManaged = new User();
        salesManagerWithNullManaged.setUsername("sm_null");
        salesManagerWithNullManaged.setPassword("password");
        salesManagerWithNullManaged.setEmail("sm_null@test.com");
        salesManagerWithNullManaged.setRole(Role.SALES_MANAGER);
        salesManagerWithNullManaged.setActive(true);
        salesManagerWithNullManaged.setCountry(Country.GERMANY);
        salesManagerWithNullManaged = userRepository.save(salesManagerWithNullManaged);

        // Create a customer with NULL sales_manager_id
        Customer customer = new Customer();
        customer.setName("Customer No SM");
        customer.setNumber("9999");
        customer.setCountry(Country.GERMANY);
        customer.setCategory("GOLD");
        customer.setActive(true);
        customer.setSalesManager(null);  // NULL sales manager
        customer = customerRepository.save(customer);

        // Create shop
        Shop shop = new Shop();
        shop.setName("Test Shop");
        shop.setNumber("SHOP001");
        shop.setCountry(Country.GERMANY);
        shop = shopRepository.save(shop);

        // Create a test purchase for this customer
        orphanPurchase = TestPurchase.builder()
                .number("TP-ORPHAN-001")
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
                .build();
        orphanPurchase = testPurchaseRepository.save(orphanPurchase);
    }

    @Test
    void customerWithNullCustomerLinkSeesNothing() {
        authenticateAs(customerUserWithNullCustomer);

        var purchases = testPurchaseService.findAllForCurrentUser();

        assertTrue(purchases.isEmpty(), "Customer with NULL customer link should see no purchases (fail closed)");
    }

    @Test
    void salesManagerSeesNoPurchasesForCustomerWithNullSalesManager() {
        authenticateAs(salesManagerWithNullManaged);

        var purchases = testPurchaseService.findAllForCurrentUser();

        assertTrue(purchases.isEmpty(), "Sales Manager should see no purchases for customers with NULL sales_manager_id");
    }

    private void authenticateAs(User user) {
        String rolePrefix = "ROLE_" + user.getRole().name();
        var auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority(rolePrefix))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
