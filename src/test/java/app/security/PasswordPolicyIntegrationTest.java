package app.security;

import app.TestPurchaseApplication;
import app.model.entity.User;
import app.repository.UserRepository;
import app.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PasswordPolicyIntegrationTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetService passwordResetService;

    @Test
    void resetPasswordRejectsShortPassword() throws Exception {
        User user = new User();
        user.setUsername("reset_short");
        user.setEmail("reset_short@example.com");
        user.setPassword(passwordEncoder.encode("LongEnough12!"));
        user.setRole(app.model.enums.Role.MYSTERY_SHOPPER);
        user.setActive(true);
        user.setCountry(app.model.enums.Country.GERMANY);
        user = userRepository.save(user);

        String token = passwordResetService.createTokenForUser(user);

        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("token", token)
                        .param("password", "short")
                        .param("confirmPassword", "short"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("at least 12 characters")));
    }
}
