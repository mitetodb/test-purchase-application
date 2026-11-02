package app.security;

import app.TestPurchaseApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHeadersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private app.client.PricingClient pricingClient;

    @Test
    void hstsSentExactlyOnceOnHttpsRequest() throws Exception {
        var result = mockMvc.perform(get("/login").secure(true))
                .andExpect(status().isOk())
                .andReturn();

        var hsts = result.getResponse().getHeaders("Strict-Transport-Security");
        assertEquals(1, hsts.size(), () -> "expected a single HSTS header, got: " + hsts);
        assertFalse(hsts.get(0).isBlank());
    }
}
