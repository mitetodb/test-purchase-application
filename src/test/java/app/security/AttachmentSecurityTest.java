package app.security;

import app.TestPurchaseApplication;
import app.model.entity.Attachment;
import app.model.entity.Customer;
import app.model.entity.Shop;
import app.model.entity.TestPurchase;
import app.model.entity.User;
import app.model.enums.*;
import app.repository.AttachmentRepository;
import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import app.repository.UserRepository;
import app.service.AttachmentService;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for attachment security: access checks and filename sanitization.
 */
@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
@Transactional
class AttachmentSecurityTest {

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private TestPurchaseRepository testPurchaseRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private TestPurchase testPurchase;
    private Attachment attachmentWithPurchase;
    private Attachment attachmentWithoutPurchase;

    @BeforeEach
    void setUp() {
        attachmentRepository.deleteAll();
        testPurchaseRepository.deleteAll();
        userRepository.deleteAll();
        customerRepository.deleteAll();

        // Create admin user
        adminUser = new User();
        adminUser.setUsername("admin_attach");
        adminUser.setPassword("password");
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(Role.ADMIN);
        adminUser.setActive(true);
        adminUser = userRepository.save(adminUser);

        // Create a customer
        Customer customer = new Customer();
        customer.setName("Test Customer");
        customer.setCountry(Country.GERMANY);
        customer.setCategory(CustomerCategory.GOLD);
        customer.setActive(true);
        customer = customerRepository.save(customer);

        // Create a shop
        Shop shop = new Shop();
        shop.setName("Test Shop");
        shop.setCountry(Country.GERMANY);
        shop = shopRepository.save(shop);

        // Create a test purchase
        testPurchase = new TestPurchase();
        testPurchase.setNumber("TP-0001");
        testPurchase.setCustomer(customer);
        testPurchase.setShop(shop);
        testPurchase.setCountry(Country.GERMANY);
        testPurchase.setCategory(Category.ACCESSORIES);
        testPurchase.setType(TestPurchaseType.ONLINE);
        testPurchase.setStatus(TestPurchaseStatus.INITIALISED);
        testPurchase.setActive(true);
        testPurchase.setProductPrice(100.0);
        testPurchase.setServiceFee(10.0);
        testPurchase.setPostageFee(5.0);
        testPurchase = testPurchaseRepository.save(testPurchase);

        // Create an attachment with a test purchase
        attachmentWithPurchase = new Attachment();
        attachmentWithPurchase.setName("test-file.pdf");
        attachmentWithPurchase.setCategory(AttachmentCategory.INVOICE);
        attachmentWithPurchase.setFileType(AttachmentFileType.PDF);
        attachmentWithPurchase.setFileSize(1024L);
        attachmentWithPurchase.setFilePath("/tmp/test-file.pdf");
        attachmentWithPurchase.setTestPurchase(testPurchase);
        attachmentWithPurchase = attachmentRepository.save(attachmentWithPurchase);

        // Create an orphan attachment without a test purchase
        attachmentWithoutPurchase = new Attachment();
        attachmentWithoutPurchase.setName("orphan-file.pdf");
        attachmentWithoutPurchase.setCategory(AttachmentCategory.INVOICE);
        attachmentWithoutPurchase.setFileType(AttachmentFileType.PDF);
        attachmentWithoutPurchase.setFileSize(512L);
        attachmentWithoutPurchase.setFilePath("/tmp/orphan-file.pdf");
        attachmentWithoutPurchase.setTestPurchase(null);
        attachmentWithoutPurchase = attachmentRepository.save(attachmentWithoutPurchase);
    }

    @Test
    void canAccessAttachmentWithTestPurchase() {
        authenticateAs(adminUser);

        Attachment result = attachmentService.findById(attachmentWithPurchase.getId());
        assertNotNull(result);
        assertEquals(attachmentWithPurchase.getId(), result.getId());
    }

    @Test
    void cannotAccessAttachmentWithoutTestPurchase() {
        authenticateAs(adminUser);

        assertThrows(IllegalArgumentException.class, () -> {
            attachmentService.findById(attachmentWithoutPurchase.getId());
        }, "Should deny access to attachment without associated test purchase");
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
