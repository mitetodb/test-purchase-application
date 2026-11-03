package app.service;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Export row cap uses a dedicated Spring context ({@code app.list-export.max-rows=2}).
 */
@SpringBootTest(
        classes = TestPurchaseApplication.class,
        properties = "app.list-export.max-rows=2"
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ListExportCapIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private TestPurchaseRepository testPurchaseRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    @Test
    void showsLocalizedMessageWhenExportCapExceeded() throws Exception {
        User admin = userRepository.save(User.builder()
                .username("cap_admin")
                .email("cap@test.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.ADMIN)
                .active(true)
                .country(Country.GERMANY)
                .managedCustomers(new HashSet<>())
                .build());
        Customer customer = customerRepository.save(Customer.builder()
                .number("99999")
                .name("Cap Customer")
                .country(Country.GERMANY)
                .active(true)
                .build());
        Shop shop = shopRepository.save(Shop.builder()
                .number("88888")
                .name("Cap Shop")
                .country(Country.GERMANY)
                .build());
        for (int i = 0; i < 3; i++) {
            TestPurchase tp = TestPurchase.builder()
                    .number("TP-CAP-" + i)
                    .customer(customer)
                    .shop(shop)
                    .country(Country.GERMANY)
                    .category(TestPurchaseCategory.M)
                    .type(TestPurchaseType.RETURN_BACK_TO_SELLER)
                    .productPrice(1.0)
                    .serviceFee(0.0)
                    .postageFee(0.0)
                    .active(true)
                    .build();
            testPurchaseRepository.saveAndFlush(tp);
        }

        MockHttpSession session = new MockHttpSession();
        var redirect = mockMvc.perform(get("/testpurchases/export.csv")
                        .session(session)
                        .with(user(userDetailsService.loadUserByUsername(admin.getUsername()))))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("exportLimitExceeded", true))
                .andExpect(flash().attribute("exportRowCount", 3L))
                .andExpect(flash().attribute("exportLimitMessage", containsString("Too many rows to export")))
                .andReturn();

        mockMvc.perform(get("/testpurchases").session((MockHttpSession) redirect.getRequest().getSession())
                        .with(user(userDetailsService.loadUserByUsername(admin.getUsername()))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Too many rows to export")));
    }
}
