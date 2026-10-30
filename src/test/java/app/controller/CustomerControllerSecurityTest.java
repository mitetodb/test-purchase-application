package app.controller;

import app.TestPurchaseApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

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
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminCanAccessCustomerList() throws Exception {
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "accountmgr", roles = {"ACCOUNT_MANAGER"})
    void accountManagerCanAccessCustomerList() throws Exception {
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "salesmgr", roles = {"SALES_MANAGER"})
    void salesManagerCanAccessCustomerList() throws Exception {
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk());
    }
}
