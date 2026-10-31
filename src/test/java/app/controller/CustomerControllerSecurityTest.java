package app.controller;

import app.TestPurchaseApplication;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashSet;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingCustomerClient pricingCustomerClient;

    private UUID adminId;
    private UUID accountManagerId;
    private UUID salesManagerId;

    @BeforeEach
    void seedAllowedUsers() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        transactionTemplate.executeWithoutResult(status -> {
            adminId = userRepository.save(User.builder()
                    .username("admin_" + suffix)
                    .email("admin_" + suffix + "@cust-sec.test")
                    .password(passwordEncoder.encode("password"))
                    .role(Role.ADMIN)
                    .country(Country.GERMANY)
                    .active(true)
                    .managedCustomers(new HashSet<>())
                    .build()).getId();
            accountManagerId = userRepository.save(User.builder()
                    .username("accountmgr_" + suffix)
                    .email("am_" + suffix + "@cust-sec.test")
                    .password(passwordEncoder.encode("password"))
                    .role(Role.ACCOUNT_MANAGER)
                    .country(Country.GERMANY)
                    .active(true)
                    .managedCustomers(new HashSet<>())
                    .build()).getId();
            salesManagerId = userRepository.save(User.builder()
                    .username("salesmgr_" + suffix)
                    .email("sm_" + suffix + "@cust-sec.test")
                    .password(passwordEncoder.encode("password"))
                    .role(Role.SALES_MANAGER)
                    .country(Country.GERMANY)
                    .active(true)
                    .managedCustomers(new HashSet<>())
                    .build()).getId();
        });
    }

    @AfterEach
    void cleanupUsers() {
        transactionTemplate.executeWithoutResult(status -> {
            if (adminId != null) {
                userRepository.deleteById(adminId);
            }
            if (accountManagerId != null) {
                userRepository.deleteById(accountManagerId);
            }
            if (salesManagerId != null) {
                userRepository.deleteById(salesManagerId);
            }
        });
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void customerCannotAccessCustomerList() throws Exception {
        mockMvc.perform(get("/customers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void customerCannotAccessAddCustomerForm() throws Exception {
        mockMvc.perform(get("/customers/add"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void customerCannotPostNewCustomer() throws Exception {
        mockMvc.perform(post("/customers/add")
                        .param("name", "Test Customer")
                        .param("country", "GERMANY")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mystery", roles = {"MYSTERY_SHOPPER"})
    void mysteryShopperCannotAccessCustomerList() throws Exception {
        mockMvc.perform(get("/customers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mystery", roles = {"MYSTERY_SHOPPER"})
    void mysteryShopperCannotPostNewCustomer() throws Exception {
        mockMvc.perform(post("/customers/add")
                        .param("name", "Test Customer")
                        .param("country", "GERMANY")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessCustomerList() throws Exception {
        User admin = userRepository.findById(adminId).orElseThrow();
        mockMvc.perform(get("/customers")
                        .with(user(userDetailsService.loadUserByUsername(admin.getUsername()))))
                .andExpect(status().isOk());
    }

    @Test
    void accountManagerCanAccessCustomerList() throws Exception {
        User accountManager = userRepository.findById(accountManagerId).orElseThrow();
        mockMvc.perform(get("/customers")
                        .with(user(userDetailsService.loadUserByUsername(accountManager.getUsername()))))
                .andExpect(status().isOk());
    }

    @Test
    void salesManagerCanAccessCustomerList() throws Exception {
        User salesManager = userRepository.findById(salesManagerId).orElseThrow();
        mockMvc.perform(get("/customers")
                        .with(user(userDetailsService.loadUserByUsername(salesManager.getUsername()))))
                .andExpect(status().isOk());
    }
}
