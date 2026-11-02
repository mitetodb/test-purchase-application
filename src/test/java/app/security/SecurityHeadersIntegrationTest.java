package app.security;

import app.TestPurchaseApplication;
import app.config.SecurityHeadersPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHeadersIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    @Test
    void loginPage_hasSingleSecurityHeadersFromApp() throws Exception {
        assertSecurityHeadersOnce(mockMvc.perform(get("/login")).andExpect(status().isOk()).andReturn());
    }

    @Test
    void staticCss_hasSingleSecurityHeadersFromApp() throws Exception {
        assertSecurityHeadersOnce(
                mockMvc.perform(get("/css/style.css")).andExpect(status().isOk()).andReturn());
    }

    @Test
    void healthEndpoint_hasSingleSecurityHeadersFromApp() throws Exception {
        assertSecurityHeadersOnce(
                mockMvc.perform(get("/actuator/health")).andExpect(status().isOk()).andReturn());
    }

    private static void assertSecurityHeadersOnce(MvcResult result) {
        assertSingleHeader(result, "Content-Security-Policy", SecurityHeadersPolicy.CONTENT_SECURITY_POLICY);
        assertThat(result.getResponse().getHeader("Content-Security-Policy"))
                .contains("frame-ancestors 'self'")
                .doesNotContain("unsafe-eval");

        assertSingleHeader(result, "X-Content-Type-Options", "nosniff");
        assertSingleHeader(result, "Referrer-Policy", SecurityHeadersPolicy.REFERRER_POLICY);
        assertSingleHeader(result, "Permissions-Policy", SecurityHeadersPolicy.PERMISSIONS_POLICY);

        assertThat(result.getResponse().getHeaders("X-Frame-Options")).isEmpty();
    }

    private static void assertSingleHeader(MvcResult result, String name, String expectedValue) {
        List<String> values = result.getResponse().getHeaders(name);
        assertThat(values)
                .as("header %s should appear exactly once", name)
                .containsExactly(expectedValue);
    }
}
