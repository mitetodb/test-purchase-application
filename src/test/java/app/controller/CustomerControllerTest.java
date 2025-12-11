package app.controller;

import app.model.entity.Customer;
import app.model.enums.Country;
import app.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void testListCustomers() throws Exception {
        Customer customer = Customer.builder()
                .id(UUID.randomUUID())
                .name("Test Customer")
                .country(Country.BULGARIA)
                .number("0001")
                .build();

        given(customerService.findAllForCurrentUser()).willReturn(List.of(customer));

        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/customers-list"))
                .andExpect(model().attributeExists("customers"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testViewCustomer() throws Exception {
        UUID customerId = UUID.randomUUID();
        Customer customer = Customer.builder()
                .id(customerId)
                .name("Test Customer")
                .country(Country.BULGARIA)
                .number("0001")
                .build();

        given(customerService.findByIdForCurrentUser(customerId)).willReturn(customer);

        mockMvc.perform(get("/customers/view/" + customerId))
                .andExpect(status().isOk())
                .andExpect(view().name("customers/customer-view"))
                .andExpect(model().attributeExists("customer"));
    }
}

