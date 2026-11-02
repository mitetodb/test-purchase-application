package app.security;

import app.TestPurchaseApplication;
import app.model.entity.User;
import app.repository.PasswordResetTokenRepository;
import app.repository.UserRepository;
import app.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
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

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private app.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    @AfterEach
    void cleanupResetFixture() {
        transactionTemplate.executeWithoutResult(status -> {
            passwordResetTokenRepository.deleteAll();
            userRepository.findByUsername("reset_short").ifPresent(u -> userRepository.deleteById(u.getId()));
        });
    }

    @Test
    void resetPasswordRejectsShortPassword() throws Exception {
        User user = transactionTemplate.execute(status -> {
            User u = new User();
            u.setUsername("reset_short");
            u.setEmail("reset_short@example.com");
            u.setPassword(passwordEncoder.encode("LongEnough12!"));
            u.setRole(app.model.enums.Role.MYSTERY_SHOPPER);
            u.setActive(true);
            u.setCountry(app.model.enums.Country.GERMANY);
            return userRepository.save(u);
        });

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
