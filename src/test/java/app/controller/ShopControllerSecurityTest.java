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
class ShopControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminCanAccessShopList() throws Exception {
        mockMvc.perform(get("/shops"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "accountmgr", roles = {"ACCOUNT_MANAGER"})
    void accountManagerCanAccessShopList() throws Exception {
        mockMvc.perform(get("/shops"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "salesmgr", roles = {"SALES_MANAGER"})
    void salesManagerCannotAccessShopList() throws Exception {
        mockMvc.perform(get("/shops"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "salesmgr", roles = {"SALES_MANAGER"})
    void salesManagerCannotAccessAddShopForm() throws Exception {
        mockMvc.perform(get("/shops/add"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "salesmgr", roles = {"SALES_MANAGER"})
    void salesManagerCannotPostNewShop() throws Exception {
        mockMvc.perform(post("/shops/add")
                        .param("name", "Test Shop")
                        .param("country", "GERMANY")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void customerCannotAccessShopList() throws Exception {
        mockMvc.perform(get("/shops"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mystery", roles = {"MYSTERY_SHOPPER"})
    void mysteryShopperCannotAccessShopList() throws Exception {
        mockMvc.perform(get("/shops"))
                .andExpect(status().isForbidden());
    }
}
