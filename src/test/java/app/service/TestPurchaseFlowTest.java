package app.service;

import app.model.dto.ItemDTO;
import app.model.dto.TestPurchaseCreateDTO;
import app.model.entity.Customer;
import app.model.entity.Shop;
import app.model.enums.Country;
import app.model.enums.TestPurchaseCategory;
import app.model.enums.TestPurchaseStatus;
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
class TestPurchaseFlowTest {

    @Autowired
    private TestPurchaseService testPurchaseService;

    @Autowired
    private StatusHistoryService statusHistoryService;

    @Autowired
    private TestPurchaseRepository testPurchaseRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Test
    void fullTestPurchaseFlow() {
        Customer customer = new Customer();
        customer.setName("Nokia");
        customer.setEmail("nokia@example.com");
        customer.setCountry(Country.BULGARIA);
        customer = customerRepository.save(customer);

        Shop shop = new Shop();
        shop.setName("store.bg");
        shop.setCountry(Country.BULGARIA);
        shop = shopRepository.save(shop);

        TestPurchaseCreateDTO dto = new TestPurchaseCreateDTO();
        dto.setCustomerId(customer.getId());
        dto.setShopId(shop.getId());
        dto.setCountry(Country.BULGARIA);
        dto.setCategory(TestPurchaseCategory.S);
        dto.setType(TestPurchaseType.FORWARDING_TO_CLIENT);

        ItemDTO item = new ItemDTO();
        item.setProductName("Phone");
        item.setQuantity(1);
        item.setUnitPrice(500.0);
        dto.setItems(List.of(item));

        // create tp
        var tp = testPurchaseService.create(dto);

        assertThat(tp.getId()).isNotNull();
        assertThat(testPurchaseRepository.count()).isEqualTo(1);

        // status change
        testPurchaseService.changeStatus(tp.getId(), TestPurchaseStatus.PRODUCT_ORDERED, "Order placed");

        var history = statusHistoryService.getHistory(tp.getId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getNewStatus()).isEqualTo(TestPurchaseStatus.PRODUCT_ORDERED);
        assertThat(history.get(0).getComment()).isEqualTo("Order placed");
    }
}