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
class TestPurchaseServiceTest {

    @Autowired
    private TestPurchaseService testPurchaseService;

    @Autowired
    private TestPurchaseRepository testPurchaseRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Test
    void testCreateTestPurchase() {
        Customer customer = new Customer();
        customer.setName("Apple");
        customer.setEmail("apple@example.com");
        customer.setCountry(Country.BULGARIA);
        customer = customerRepository.save(customer);

        Shop shop = new Shop();
        shop.setName("apple.bg");
        shop.setCountry(Country.BULGARIA);
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
