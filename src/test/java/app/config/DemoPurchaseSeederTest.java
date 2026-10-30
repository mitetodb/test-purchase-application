package app.config;

import app.client.PricingClient;
import app.client.PricingCustomerClient;
import app.model.dto.PriceCalculationResponseDTO;
import app.model.enums.TestPurchaseStatus;
import app.model.enums.TestPurchaseType;
import app.repository.TestPurchaseRepository;
import app.repository.TestPurchaseStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {"app.demo-data.enabled=true", "app.demo-data.purchases=24",
        "app.init-data.admin-password=test-admin-pw"})
@ActiveProfiles("test")
class DemoPurchaseSeederTest {

    @MockBean PricingClient pricingClient;
    @MockBean PricingCustomerClient pricingCustomerClient;
    @Autowired TestPurchaseRepository purchases;
    @Autowired TestPurchaseStatusHistoryRepository history;
    @Autowired DemoPurchaseSeeder seeder;
    @Autowired app.service.UserService userService;

    @Test
    void seedsAllStatusesWithConsistentHistoryAndIsIdempotent() throws Exception {
        // the runner ran at startup with the mock returning null -> it aborts cleanly; stub and run again
        PriceCalculationResponseDTO r = new PriceCalculationResponseDTO();
        r.setTestPurchaseFee(30.0);
        r.setPostageFee(15.0);
        when(pricingClient.calculatePrice(any())).thenReturn(r);
        if (userService.countUsers() == 0) {
            app.model.dto.UserCreateDTO u = new app.model.dto.UserCreateDTO();
            u.setUsername("admin"); u.setPassword("test-admin-pw"); u.setConfirmPassword("test-admin-pw");
            u.setEmail("admin@example.com"); u.setRole(app.model.enums.Role.ADMIN);
            u.setCountry(app.model.enums.Country.BULGARIA);
            u.setActive(true);
            userService.createUser(u);
        }
        long before = purchases.count();
        seeder.run();
        var all = purchases.findAll();
        assertThat(all.size() - before).isEqualTo(24);
        var statuses = all.stream().map(p -> p.getStatus()).collect(Collectors.toSet());
        assertThat(statuses).containsAll(Arrays.asList(TestPurchaseStatus.values()));
        all.forEach(p -> {
            var h = history.findByTestPurchaseIdOrderByChangedAtDesc(p.getId());
            if (p.getStatus() == TestPurchaseStatus.INITIALISED) assertThat(h).isEmpty();
            else assertThat(h.get(0).getNewStatus()).isEqualTo(p.getStatus());
            if (p.getStatus() == TestPurchaseStatus.PRODUCT_FORWARDED_TO_CLIENT)
                assertThat(p.getType()).isEqualTo(TestPurchaseType.FORWARDING_TO_CLIENT);
            assertThat(p.getServiceFee()).isEqualTo(30.0);
        });
        seeder.run();
        assertThat(purchases.count()).isEqualTo(all.size());
    }
}
