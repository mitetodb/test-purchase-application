package app.security;

import app.TestPurchaseApplication;
import app.model.entity.LoginRateLimit;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.LoginRateLimitRepository;
import app.repository.UserRepository;
import app.security.LoginRateLimitKeys;
import app.service.LoginSecurityService;
import app.service.ScheduledTasksService;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LoginSecurityIntegrationTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private LoginSecurityService loginSecurityService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private LoginRateLimitRepository loginRateLimitRepository;

    @Autowired
    private ScheduledTasksService scheduledTasksService;

    @BeforeEach
    void clearRateLimits() {
        transactionTemplate.executeWithoutResult(status -> loginRateLimitRepository.deleteAll());
    }

    @Test
    void accountLocksAfterConfiguredFailedAttempts() throws Exception {
        User user = persistUser("lockout_target", "CorrectPass1!");

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/login")
                            .with(csrf())
                            .param("username", user.getUsername())
                            .param("password", "wrong-password"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error"));
        }

        User locked = reload(user.getId());
        assertNotNull(locked.getLockedUntil());
        assertTrue(locked.getLockedUntil().isAfter(LocalDateTime.now()));
        assertEquals(3, locked.getFailedLoginAttempts());
        assertTrue(loginSecurityService.isCurrentlyLocked(locked));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", user.getUsername())
                        .param("password", "CorrectPass1!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        mockMvc.perform(get("/login?error"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid username or password")));
    }

    @Test
    void rateLimitBlocksFurtherAttemptsPerIpAndUsername() throws Exception {
        String username = "rate_limit_user";
        persistUser(username, "SecretPass1!");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/login")
                            .with(csrf())
                            .param("username", username)
                            .param("password", "bad"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error"));
        }

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", username)
                        .param("password", "bad"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        assertTrue(loginSecurityService.isRateLimitExceeded("127.0.0.1", username));
    }

    @Test
    void rateLimitPerIpAppliesAcrossDifferentUsernames() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/login")
                            .with(csrf())
                            .param("username", "user_" + i)
                            .param("password", "bad"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error"));
        }

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "another_user")
                        .param("password", "bad"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        assertTrue(loginSecurityService.isRateLimitExceeded("127.0.0.1", "another_user"));
    }

    @Test
    void spoofedXForwardedForDoesNotBypassPerIpRateLimit() throws Exception {
        String username = "xff_spoof_user";
        persistUser(username, "SecretPass1!");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/login")
                            .with(csrf())
                            .param("username", username)
                            .param("password", "bad"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login?error"));
        }

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .header("X-Forwarded-For", "198.51.100.50")
                        .param("username", username)
                        .param("password", "bad"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        assertTrue(loginSecurityService.isRateLimitExceeded("127.0.0.1", username));
    }

    @Test
    void veryLongUsernameLoginFailsGracefullyWithoutServerError() throws Exception {
        String longUsername = "x".repeat(1000);

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", longUsername)
                        .param("password", "any"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void adminCanUnlockAccount() throws Exception {
        User user = persistUser("unlock_me", "GoodPass1!");
        transactionTemplate.executeWithoutResult(status -> {
            user.setFailedLoginAttempts(3);
            user.setLockedUntil(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
        });

        User admin = persistUser("unlock_admin", "AdminPass1!", Role.ADMIN);

        mockMvc.perform(post("/users/unlock/" + user.getId())
                        .with(user(admin.getUsername()).roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users"));

        User unlocked = reload(user.getId());
        assertNull(unlocked.getLockedUntil());
        assertEquals(0, unlocked.getFailedLoginAttempts());
        assertFalse(loginSecurityService.isCurrentlyLocked(unlocked));
    }

    @Test
    void nonAdminUnlockReturns403() throws Exception {
        User user = persistUser("locked_nonadmin", "Pass1234!");
        transactionTemplate.executeWithoutResult(status -> {
            user.setFailedLoginAttempts(3);
            user.setLockedUntil(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
        });

        User accountManager = persistUser("am_unlock", "AmPass1234!", Role.ACCOUNT_MANAGER);

        mockMvc.perform(post("/users/unlock/" + user.getId())
                        .with(user(accountManager.getUsername()).roles("ACCOUNT_MANAGER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUnlockWithoutCsrfReturns403() throws Exception {
        User user = persistUser("locked_no_csrf", "Pass1234!");
        transactionTemplate.executeWithoutResult(status -> {
            user.setFailedLoginAttempts(3);
            user.setLockedUntil(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
        });

        User admin = persistUser("admin_no_csrf", "AdminPass1!", Role.ADMIN);

        mockMvc.perform(post("/users/unlock/" + user.getId())
                        .with(user(admin.getUsername()).roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void successfulLoginResetsFailedAttempts() throws Exception {
        User user = persistUser("reset_on_success", "MyPass123!");
        transactionTemplate.executeWithoutResult(status -> {
            user.setFailedLoginAttempts(2);
            userRepository.save(user);
        });

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .header("X-Real-IP", "10.0.0.55")
                        .param("username", user.getUsername())
                        .param("password", "MyPass123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));

        User refreshed = reload(user.getId());
        assertEquals(0, refreshed.getFailedLoginAttempts());
        assertNull(refreshed.getLockedUntil());
    }

    @Test
    void scheduledTaskPurgesExpiredRateLimitRows() {
        LocalDateTime expired = LocalDateTime.now().minusHours(2);
        transactionTemplate.executeWithoutResult(status -> loginRateLimitRepository.save(
                LoginRateLimit.builder()
                        .rateKey(LoginRateLimitKeys.storageKey("ip:", "127.0.0.1"))
                        .attemptCount(5)
                        .windowStart(expired)
                        .build()));

        scheduledTasksService.cleanupExpiredLoginRateLimits();

        assertEquals(0, loginRateLimitRepository.count());
    }

    private User persistUser(String username, String rawPassword) {
        return persistUser(username, rawPassword, Role.MYSTERY_SHOPPER);
    }

    private User persistUser(String username, String rawPassword, Role role) {
        return transactionTemplate.execute(status -> {
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

    private User reload(UUID id) {
        return transactionTemplate.execute(status -> userRepository.findById(id).orElseThrow());
    }
}
