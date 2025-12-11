package app.security;

import app.TestPurchaseApplication;
import app.service.TestPurchaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatusSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TestPurchaseService testPurchaseService;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminCanChangeStatus() throws Exception {
        doNothing().when(testPurchaseService).changeStatus(any(UUID.class), any(), any());

        mockMvc.perform(post("/testpurchases/" + UUID.randomUUID() + "/change-status")
                        .param("newStatus", "PRODUCT_ORDERED")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void customerCannotChangeStatus() throws Exception {
        mockMvc.perform(post("/testpurchases/" + UUID.randomUUID() + "/change-status")
                        .param("newStatus", "PRODUCT_ORDERED"))
                .andExpect(status().isForbidden());
    }
}
