package app.security;

import app.TestPurchaseApplication;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.LoginRateLimitRepository;
import app.repository.UserRepository;
import app.service.impl.TwoFactorServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = {
        "app.security.two-factor.max-session-failed-attempts=3",
        "app.security.login.max-failed-attempts=50"
})
class TotpSessionFailureLimitIntegrationTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TwoFactorServiceImpl twoFactorService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private LoginRateLimitRepository loginRateLimitRepository;

    @BeforeEach
    void resetSecurityState() {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                loginRateLimitRepository.deleteAll();
            } catch (org.springframework.dao.DataAccessException ignored) {
            }
        });
    }

    @AfterEach
    void deleteCreatedUsers() {
        transactionTemplate.executeWithoutResult(status ->
                userRepository.findAll().stream()
                        .filter(u -> u.getUsername().startsWith("totp_sess_fail_"))
                        .forEach(u -> userRepository.deleteById(u.getId())));
    }

    @Test
    void pendingTwoFactorSessionInvalidatedAfterMaxFailures() throws Exception {
        String suffix = java.util.UUID.randomUUID().toString().substring(0, 8);
        User admin = persistUser("totp_sess_fail_" + suffix, "AdminPass1234!", Role.ADMIN);
        MvcResult login = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", admin.getUsername())
                        .param("password", "AdminPass1234!"))
                .andExpect(redirectedUrl("/2fa/enroll"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();
        session.setAttribute(TwoFactorSessionKeys.ENROLLMENT_SECRET, twoFactorService.generateSecret());

        mockMvc.perform(post("/2fa/enroll").session(session).with(csrf()).param("code", "000000"))
                .andExpect(redirectedUrl("/2fa/enroll"));
        mockMvc.perform(post("/2fa/enroll").session(session).with(csrf()).param("code", "000000"))
                .andExpect(redirectedUrl("/2fa/enroll"));
        mockMvc.perform(post("/2fa/enroll").session(session).with(csrf()).param("code", "000000"))
                .andExpect(redirectedUrl("/login?error"));
    }

    private User persistUser(String username, String rawPassword, Role role) {
        return transactionTemplate.execute(status -> {
            userRepository.findByUsername(username).ifPresent(u -> userRepository.deleteById(u.getId()));
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setEmail(username + "@example.com");
            user.setRole(role);
            user.setActive(true);
            user.setCountry(Country.GERMANY);
            return userRepository.save(user);
        });
    }
}
