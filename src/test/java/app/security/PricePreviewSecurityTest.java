package app.security;

import app.TestPurchaseApplication;
import app.model.dto.TestPurchaseCreateDTO;
import app.model.entity.Customer;
import app.model.entity.Shop;
import app.model.entity.User;
import app.model.enums.*;
import app.repository.CustomerRepository;
import app.repository.ShopRepository;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify CUSTOMER users can only preview prices for their own company.
 */
@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
@Transactional
class PricePreviewSecurityTest {

    @Autowired
    private TestPurchaseService testPurchaseService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    private User customerUser;
    private Customer ownCustomer;
    private Customer otherCustomer;
    private Shop shop;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        customerRepository.deleteAll();

        // Create two customers
        ownCustomer = new Customer();
        ownCustomer.setName("Own Customer");
        ownCustomer.setCountry(Country.GERMANY);
        ownCustomer.setCategory(CustomerCategory.GOLD);
        ownCustomer.setActive(true);
        ownCustomer = customerRepository.save(ownCustomer);

        otherCustomer = new Customer();
        otherCustomer.setName("Other Customer");
        otherCustomer.setCountry(Country.GERMANY);
        otherCustomer.setCategory(CustomerCategory.SILVER);
        otherCustomer.setActive(true);
        otherCustomer = customerRepository.save(otherCustomer);

        // Create customer user linked to ownCustomer
        customerUser = new User();
        customerUser.setUsername("customer_price");
        customerUser.setPassword("password");
        customerUser.setEmail("customer@test.com");
        customerUser.setRole(Role.CUSTOMER);
        customerUser.setCustomer(ownCustomer);
        customerUser.setActive(true);
        customerUser = userRepository.save(customerUser);

        // Create a shop
        shop = new Shop();
        shop.setName("Test Shop");
        shop.setCountry(Country.GERMANY);
        shop = shopRepository.save(shop);
    }

    @Test
    void customerCanPreviewPriceForOwnCompany() {
        authenticateAs(customerUser);

        TestPurchaseCreateDTO dto = new TestPurchaseCreateDTO();
        dto.setCustomerId(ownCustomer.getId());
        dto.setShopId(shop.getId());
        dto.setCountry(Country.GERMANY);
        dto.setCategory(Category.ACCESSORIES);
        dto.setType(TestPurchaseType.ONLINE);
        dto.getItems().add(createItemDTO("Product", 1, 100.0));

        assertDoesNotThrow(() -> {
            testPurchaseService.previewPrice(dto);
        });
    }

    @Test
    void customerCannotPreviewPriceForOtherCompany() {
        authenticateAs(customerUser);

        TestPurchaseCreateDTO dto = new TestPurchaseCreateDTO();
        dto.setCustomerId(otherCustomer.getId());
        dto.setShopId(shop.getId());
        dto.setCountry(Country.GERMANY);
        dto.setCategory(Category.ACCESSORIES);
        dto.setType(TestPurchaseType.ONLINE);
        dto.getItems().add(createItemDTO("Product", 1, 100.0));

        assertThrows(AccessDeniedException.class, () -> {
            testPurchaseService.previewPrice(dto);
        }, "Customer should not be able to preview prices for another company");
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
