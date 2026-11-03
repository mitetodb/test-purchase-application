package app.controller;

import app.model.entity.Customer;
import app.model.enums.Country;
import app.service.CustomerService;
import app.security.CustomUserDetailsService;
import app.security.LoginAuthenticationFailureHandler;
import app.security.LoginAuthenticationSuccessHandler;
import app.security.LoginRateLimitFilter;
import app.service.LoginSecurityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest(
        controllers = CustomerController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @MockBean
    private LoginSecurityService loginSecurityService;

    @MockBean
    private LoginRateLimitFilter loginRateLimitFilter;

    @MockBean
    private LoginAuthenticationFailureHandler loginAuthenticationFailureHandler;

    @MockBean
    private LoginAuthenticationSuccessHandler loginAuthenticationSuccessHandler;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private app.security.TwoFactorGateFilter twoFactorGateFilter;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testListCustomers() throws Exception {
        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .name("Test Customer")
                .country(Country.BULGARIA)
                .number("0001")
                .build();

        given(customerService.findAllForCurrentUser(anyBoolean())).willReturn(List.of(customer));

        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("customers"));
    }

}

