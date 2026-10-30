package app.service;

import app.model.dto.ItemDTO;
import app.model.dto.TestPurchaseCreateDTO;
import app.model.entity.Customer;
import app.model.entity.Shop;
import app.model.entity.TestPurchase;
import app.model.enums.Country;
import app.model.enums.TestPurchaseCategory;
import app.model.enums.TestPurchaseType;
import app.repository.CustomerRepository;
import app.repository.ShopRepository;
import app.repository.TestPurchaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
class TestPurchaseServiceTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    @org.junit.jupiter.api.BeforeEach
    void stubPricing() {
        app.model.dto.PriceCalculationResponseDTO r = new app.model.dto.PriceCalculationResponseDTO();
        r.setTestPurchaseFee(150.0);
        r.setPostageFee(10.0);
        r.setProductPrice(99.0);
        org.mockito.Mockito.when(pricingClient.calculatePrice(org.mockito.ArgumentMatchers.any())).thenReturn(r);
    }

    @Autowired
    private TestPurchaseService testPurchaseService;

    @Autowired
    private TestPurchaseRepository testPurchaseRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private jakarta.persistence.EntityManagerFactory entityManagerFactory;

    @Autowired
    private app.repository.UserRepository userRepository;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        // Force schema creation by accessing the database
        try (jakarta.persistence.EntityManager em = entityManagerFactory.createEntityManager()) {
            em.getMetamodel();
        }
        
        // Create admin user if not exists
        if (userRepository.findByUsername("admin").isEmpty()) {
            app.model.entity.User admin = app.model.entity.User.builder()
                    .username("admin")
                    .email("admin@test.com")
                    .password(passwordEncoder.encode("password"))
                    .role(app.model.enums.Role.ADMIN)
                    .active(true)
                    .build();
            userRepository.save(admin);
        }
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = {"ADMIN"})
    void testCreateTestPurchase() {
        Customer customer = new Customer();
        customer.setName("Apple");
        customer.setEmail("apple@example.com");
        customer.setCountry(Country.BULGARIA);
        customer.setNumber("0001");
        customer.setActive(true);
        customer = customerRepository.save(customer);

        Shop shop = new Shop();
        shop.setName("apple.bg");
        shop.setCountry(Country.BULGARIA);
        shop.setNumber("0001");
        shop = shopRepository.save(shop);

        TestPurchaseCreateDTO dto = new TestPurchaseCreateDTO();
        dto.setCustomerId(customer.getId());
        dto.setShopId(shop.getId());
        dto.setCountry(Country.BULGARIA);
        dto.setCategory(TestPurchaseCategory.S);
        dto.setType(TestPurchaseType.FORWARDING_TO_CLIENT);

        ItemDTO item = new ItemDTO();
        item.setProductName("MacBook Pro");
        item.setQuantity(1);
        item.setUnitPrice(1000.0);
        dto.setItems(List.of(item));

        TestPurchase saved = testPurchaseService.create(dto);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getNumber()).isNotBlank();
        assertThat(testPurchaseRepository.count()).isEqualTo(1);
        assertThat(saved.getProductPrice()).isEqualTo(1000.0);
    }
}
