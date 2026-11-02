package app.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UnknownUrlNotFoundMvcTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private app.client.PricingClient pricingClient;

    @Test
    void anonymousUnknownUrlReturns404NotLoginRedirect() throws Exception {
        mvc.perform(get("/no-such-page-" + System.nanoTime()))
                .andExpect(status().isNotFound())
                .andExpect(forwardedUrl("/error"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void authenticatedUnknownUrlReturns404() throws Exception {
        mvc.perform(get("/no-such-page-" + System.nanoTime()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/not-found"));
    }

    @Test
    void anonymousMissingStaticFileReturns404() throws Exception {
        mvc.perform(get("/css/no-such-stylesheet-" + System.nanoTime() + ".css"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousHomeRedirectsToLogin() throws Exception {
        mvc.perform(get("/home"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void anonymousDeleteHomeDoesNotReturn405() throws Exception {
        mvc.perform(delete("/home").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void anonymousUsersListRedirectsToLoginWithoutData() throws Exception {
        mvc.perform(get("/users/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"))
                .andExpect(content().string(not(containsString("users-list"))));
    }

    @Test
    void anonymousUpperCaseUsersPathIsNotReachable() throws Exception {
        mvc.perform(get("/USERS"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("users-list"))));
    }

    @Test
    void anonymousProtectedKnownRouteRedirectsToLogin() throws Exception {
        mvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void anonymousTestPurchasesRedirectsToLogin() throws Exception {
        mvc.perform(get("/testpurchases"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
