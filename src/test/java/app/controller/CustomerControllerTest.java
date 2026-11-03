package app.controller;

import app.config.AppListExportProperties;
import app.model.entity.Customer;
import app.model.enums.Country;
import app.service.CustomerService;
import app.support.ListExportResponseFactory;
import app.support.ListPagingSupport;
import app.service.UploadErrorMessages;
import app.service.UserService;
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

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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
    private ListPagingSupport listPagingSupport;

    @MockBean
    private AppListExportProperties exportProperties;

    @MockBean
    private ListExportResponseFactory exportResponses;

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
    private UploadErrorMessages uploadErrorMessages;

    @MockBean
    private UserService userService;

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

        given(customerService.findPageForCurrentUser(any(), any()))
                .willReturn(new PageImpl<>(List.of(customer), PageRequest.of(0, 20), 1));
        given(listPagingSupport.forCustomers(any())).willReturn(PageRequest.of(0, 20));

        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("customers"))
                .andExpect(model().attributeExists("customerPage"));
    }

}
