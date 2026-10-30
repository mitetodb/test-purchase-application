package app.security;

import app.TestPurchaseApplication;
import app.model.dto.TestPurchaseCreateDTO;
import app.model.dto.TestPurchaseEditDTO;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify Account Manager can only edit and create test purchases
 * for customers they manage, preventing IDOR attacks.
 */
@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
@Transactional
class AccountManagerScopeTest {

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

    private User accountManager1;
    private User accountManager2;
    private Customer managedCustomer;
    private Customer unmanagedCustomer;
    private Shop shop;
    private TestPurchase managedPurchase;
    private TestPurchase unmanagedPurchase;

    @BeforeEach
    void setUp() {
        testPurchaseRepository.deleteAll();
        userRepository.deleteAll();
        customerRepository.deleteAll();

        // Create two customers
        managedCustomer = new Customer();
        managedCustomer.setName("Managed Customer");
        managedCustomer.setCountry(Country.GERMANY);
        managedCustomer.setCategory("GOLD");
        managedCustomer.setActive(true);
        managedCustomer = customerRepository.save(managedCustomer);

        unmanagedCustomer = new Customer();
        unmanagedCustomer.setName("Unmanaged Customer");
        unmanagedCustomer.setCountry(Country.GERMANY);
        unmanagedCustomer.setCategory("SILVER");
        unmanagedCustomer.setActive(true);
        unmanagedCustomer = customerRepository.save(unmanagedCustomer);

        // Create Account Manager 1 who manages managedCustomer
        accountManager1 = new User();
        accountManager1.setUsername("am1_scope");
        accountManager1.setPassword("password");
        accountManager1.setEmail("am1@test.com");
        accountManager1.setRole(Role.ACCOUNT_MANAGER);
        accountManager1.setActive(true);
        accountManager1 = userRepository.save(accountManager1);
        accountManager1.getManagedCustomers().add(managedCustomer);
        accountManager1 = userRepository.save(accountManager1);

        // Create Account Manager 2 who manages unmanagedCustomer
        accountManager2 = new User();
        accountManager2.setUsername("am2_scope");
        accountManager2.setPassword("password");
        accountManager2.setEmail("am2@test.com");
        accountManager2.setRole(Role.ACCOUNT_MANAGER);
        accountManager2.setActive(true);
        accountManager2 = userRepository.save(accountManager2);
        accountManager2.getManagedCustomers().add(unmanagedCustomer);
        accountManager2 = userRepository.save(accountManager2);

        // Create a shop
        shop = new Shop();
        shop.setName("Test Shop");
        shop.setCountry(Country.GERMANY);
        shop = shopRepository.save(shop);

        // Create test purchases
        managedPurchase = new TestPurchase();
        managedPurchase.setNumber("TP-0001");
        managedPurchase.setCustomer(managedCustomer);
        managedPurchase.setShop(shop);
        managedPurchase.setCountry(Country.GERMANY);
        managedPurchase.setCategory(TestPurchaseCategory.S);
        managedPurchase.setType(TestPurchaseType.FORWARDING_TO_CLIENT);
        managedPurchase.setStatus(TestPurchaseStatus.INITIALISED);
        managedPurchase.setActive(true);
        managedPurchase.setProductPrice(100.0);
        managedPurchase.setServiceFee(10.0);
        managedPurchase.setPostageFee(5.0);
        managedPurchase = testPurchaseRepository.save(managedPurchase);

        unmanagedPurchase = new TestPurchase();
        unmanagedPurchase.setNumber("TP-0002");
        unmanagedPurchase.setCustomer(unmanagedCustomer);
        unmanagedPurchase.setShop(shop);
        unmanagedPurchase.setCountry(Country.GERMANY);
        unmanagedPurchase.setCategory(TestPurchaseCategory.S);
        unmanagedPurchase.setType(TestPurchaseType.FORWARDING_TO_CLIENT);
        unmanagedPurchase.setStatus(TestPurchaseStatus.INITIALISED);
        unmanagedPurchase.setActive(true);
        unmanagedPurchase.setProductPrice(200.0);
        unmanagedPurchase.setServiceFee(20.0);
        unmanagedPurchase.setPostageFee(10.0);
        unmanagedPurchase = testPurchaseRepository.save(unmanagedPurchase);
    }

    @Test
    void accountManagerCanEditManagedTestPurchase() {
        authenticateAs(accountManager1);

        TestPurchaseEditDTO dto = new TestPurchaseEditDTO();
        dto.setCustomerId(managedCustomer.getId());
        dto.setShopId(shop.getId());
        dto.setCountry(Country.FRANCE);
        dto.setCategory(TestPurchaseCategory.M);
        dto.setType(TestPurchaseType.RETURN_BACK_TO_SELLER);

        assertDoesNotThrow(() -> {
            testPurchaseService.edit(managedPurchase.getId(), dto);
        });
    }

    @Test
    void accountManagerCannotEditUnmanagedTestPurchase() {
        authenticateAs(accountManager1);

        TestPurchaseEditDTO dto = new TestPurchaseEditDTO();
        dto.setCustomerId(unmanagedCustomer.getId());
        dto.setShopId(shop.getId());
        dto.setCountry(Country.FRANCE);
        dto.setCategory(TestPurchaseCategory.M);
        dto.setType(TestPurchaseType.RETURN_BACK_TO_SELLER);

        assertThrows(AccessDeniedException.class, () -> {
            testPurchaseService.edit(unmanagedPurchase.getId(), dto);
        }, "Account Manager should not be able to edit unmanaged test purchase");
    }

    @Test
    void accountManagerCannotReassignTestPurchaseToUnmanagedCustomer() {
        authenticateAs(accountManager1);

        TestPurchaseEditDTO dto = new TestPurchaseEditDTO();
        dto.setCustomerId(unmanagedCustomer.getId()); // Trying to change to unmanaged customer
        dto.setShopId(shop.getId());
        dto.setCountry(Country.GERMANY);
        dto.setCategory(TestPurchaseCategory.S);
        dto.setType(TestPurchaseType.FORWARDING_TO_CLIENT);

        assertThrows(AccessDeniedException.class, () -> {
            testPurchaseService.edit(managedPurchase.getId(), dto);
        }, "Account Manager should not be able to reassign test purchase to unmanaged customer");
    }

    @Test
    void accountManagerCanCreateTestPurchaseForManagedCustomer() {
        authenticateAs(accountManager1);

        TestPurchaseCreateDTO dto = new TestPurchaseCreateDTO();
        dto.setCustomerId(managedCustomer.getId());
        dto.setShopId(shop.getId());
        dto.setCountry(Country.GERMANY);
        dto.setCategory(TestPurchaseCategory.S);
        dto.setType(TestPurchaseType.FORWARDING_TO_CLIENT);
        dto.getItems().add(createItemDTO("Product", 1, 50.0));

        assertDoesNotThrow(() -> {
            testPurchaseService.create(dto);
        });
    }

    @Test
    void accountManagerCannotCreateTestPurchaseForUnmanagedCustomer() {
        authenticateAs(accountManager1);

        TestPurchaseCreateDTO dto = new TestPurchaseCreateDTO();
        dto.setCustomerId(unmanagedCustomer.getId());
        dto.setShopId(shop.getId());
        dto.setCountry(Country.GERMANY);
        dto.setCategory(TestPurchaseCategory.S);
        dto.setType(TestPurchaseType.FORWARDING_TO_CLIENT);
        dto.getItems().add(createItemDTO("Product", 1, 50.0));

        assertThrows(AccessDeniedException.class, () -> {
            testPurchaseService.create(dto);
        }, "Account Manager should not be able to create test purchase for unmanaged customer");
    }

    @Test
    void accountManagerCannotAccessEditFormForUnmanagedTestPurchase() {
        authenticateAs(accountManager1);

        assertThrows(AccessDeniedException.class, () -> {
            testPurchaseService.findByIdForEdit(unmanagedPurchase.getId());
        }, "Account Manager should not be able to access edit form for unmanaged test purchase");
    }

    @Test
    void accountManagerCanAccessEditFormForManagedTestPurchase() {
        authenticateAs(accountManager1);

        TestPurchase result = testPurchaseService.findByIdForEdit(managedPurchase.getId());
        assertNotNull(result);
        assertEquals(managedPurchase.getId(), result.getId());
    }

    private app.model.dto.ItemDTO createItemDTO(String name, int qty, double price) {
        app.model.dto.ItemDTO item = new app.model.dto.ItemDTO();
        item.setProductName(name);
        item.setQuantity(qty);
        item.setUnitPrice(price);
        return item;
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
