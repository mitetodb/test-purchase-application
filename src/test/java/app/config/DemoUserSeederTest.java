package app.config;

import app.client.PricingClient;
import app.client.PricingCustomerClient;
import app.model.enums.Role;
import app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"app.demo-data.enabled=true", "app.demo-data.users=true"})
@ActiveProfiles("test")
class DemoUserSeederTest {

    @MockBean PricingClient pricingClient;
    @MockBean PricingCustomerClient pricingCustomerClient;
    @Autowired UserRepository users;
    @Autowired DemoUserSeeder seeder;

    @Test
    void createsEveryNonAdminRoleAndIsIdempotent() {
        seeder.run();
        long count = users.count();
        var roles = users.findAll().stream().filter(u -> u.getUsername().startsWith("demo."))
                .map(u -> u.getRole()).collect(Collectors.toSet());
        assertThat(roles).containsAll(Arrays.stream(Role.values()).filter(r -> r != Role.ADMIN).toList());
        seeder.run();
        assertThat(users.count()).isEqualTo(count);
    }
}
