package app.security;

import app.TestPurchaseApplication;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.UserRepository;
import app.repository.LoginRateLimitRepository;
import app.service.impl.TwoFactorServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TotpSecurityIntegrationTest {

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
                // H2 schema may not be initialized yet when tests run in unusual order
            }
            userRepository.findAll().stream()
                    .filter(u -> u.getUsername().startsWith("totp_test_"))
                    .forEach(u -> {
                        u.setFailedLoginAttempts(0);
                        u.setLockedUntil(null);
                        userRepository.save(u);
                    });
        });
    }

    @AfterEach
    void deleteCreatedUsers() {
        transactionTemplate.executeWithoutResult(status ->
                userRepository.findAll().stream()
                        .filter(u -> u.getUsername().startsWith("totp_test_"))
                        .forEach(u -> userRepository.deleteById(u.getId())));
    }

    @Test
    void mysteryShopperLoginSkipsTwoFactor() throws Exception {
        User user = persistUser("totp_test_ms", "ShopperPass12!", Role.MYSTERY_SHOPPER);

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", user.getUsername())
                        .param("password", "ShopperPass12!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

    @Test
    void adminWithoutTotpForcedToEnrollAfterLogin() throws Exception {
        User admin = persistUser("totp_test_enroll", "AdminPass1234!", Role.ADMIN);

        MvcResult login = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", admin.getUsername())
                        .param("password", "AdminPass1234!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/2fa/enroll"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();
        mockMvc.perform(get("/dashboard").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/2fa/enroll"));
    }

    @Test
    void uploadsBlockedUntilTwoFactorComplete() throws Exception {
        User admin = persistUser("totp_test_upload", "AdminPass1234!", Role.ADMIN);
        MvcResult login = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", admin.getUsername())
                        .param("password", "AdminPass1234!"))
                .andExpect(redirectedUrl("/2fa/enroll"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();

        mockMvc.perform(get("/uploads/avatars/example.png").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/2fa/enroll"));

        mockMvc.perform(get("/avatars/example.png").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/2fa/enroll"));
    }

    @Test
    @WithMockUser(username = "totp_test_orphan", roles = "ADMIN")
    void enrollWithoutPasswordLoginSessionRedirectsToLoginNot500() throws Exception {
        persistUser("totp_test_orphan", "AdminPass1234!", Role.ADMIN);
        mockMvc.perform(get("/2fa/enroll"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void enrollmentCodeCannotBeReused() throws Exception {
        User admin = persistUser("totp_test_reuse_enroll", "AdminPass1234!", Role.ADMIN);
        String secret = twoFactorService.generateSecret();
        String code = twoFactorService.generateCurrentCode(secret);

        MvcResult login = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", admin.getUsername())
                        .param("password", "AdminPass1234!"))
                .andExpect(redirectedUrl("/2fa/enroll"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();
        session.setAttribute(TwoFactorSessionKeys.ENROLLMENT_SECRET, secret);

        mockMvc.perform(post("/2fa/enroll").session(session).with(csrf()).param("code", code))
                .andExpect(redirectedUrl("/2fa/recovery-codes"));

        User reloaded = userRepository.findByUsername(admin.getUsername()).orElseThrow();
        assertTrue(reloaded.isTotpEnabled());
        assertNotNull(reloaded.getTotpLastTimeStep());

        MockHttpSession session2 = (MockHttpSession) mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", admin.getUsername())
                        .param("password", "AdminPass1234!"))
                .andExpect(redirectedUrl("/2fa/verify"))
                .andReturn()
                .getRequest()
                .getSession();

        assertFalse(twoFactorService.verifyTotpCode(reloaded, code));
    }

    @Test
    void totpReplayRejectedForSameMatchedTimeStep() throws Exception {
        String secret = twoFactorService.generateSecret();
        User admin = persistUser("totp_test_replay", "AdminPass1234!", Role.ADMIN);
        String enrollCode = twoFactorService.generateCurrentCode(secret);
        transactionTemplate.executeWithoutResult(status ->
                twoFactorService.completeEnrollmentAndIssueRecoveryCodes(
                        userRepository.findByUsername(admin.getUsername()).orElseThrow(), secret, enrollCode));

        MvcResult login = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", admin.getUsername())
                        .param("password", "AdminPass1234!"))
                .andExpect(redirectedUrl("/2fa/verify"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();
        User enrolled = userRepository.findByUsername(admin.getUsername()).orElseThrow();
        long step = enrolled.getTotpLastTimeStep();
        String verifyCode = twoFactorService.generateCodeForTimeStep(secret, step + 1);

        mockMvc.perform(post("/2fa/verify").session(session).with(csrf()).param("code", verifyCode))
                .andExpect(redirectedUrl("/home"));

        MvcResult login2 = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", admin.getUsername())
                        .param("password", "AdminPass1234!"))
                .andExpect(redirectedUrl("/2fa/verify"))
                .andReturn();
        MockHttpSession session2 = (MockHttpSession) login2.getRequest().getSession();

        mockMvc.perform(post("/2fa/verify").session(session2).with(csrf()).param("code", verifyCode))
                .andExpect(redirectedUrl("/2fa/verify"));
    }

    @Test
    void totpAcceptsCodeInNextTimeStepAfterPreviousStepUsed() {
        String secret = twoFactorService.generateSecret();
        User admin = persistUser("totp_test_next_step", "AdminPass1234!", Role.ADMIN);
        long step = twoFactorService.currentTimeStep();
        String codeStep1 = twoFactorService.generateCodeForTimeStep(secret, step);
        twoFactorService.completeEnrollmentAndIssueRecoveryCodes(
                userRepository.findByUsername(admin.getUsername()).orElseThrow(), secret, codeStep1);

        User enrolled = userRepository.findByUsername(admin.getUsername()).orElseThrow();
        assertEquals(step, enrolled.getTotpLastTimeStep());

        String codeStep2 = twoFactorService.generateCodeForTimeStep(secret, step + 1);
        assertTrue(twoFactorService.verifyTotpCode(enrolled, codeStep2));

        User after = userRepository.findByUsername(admin.getUsername()).orElseThrow();
        assertEquals(step + 1, after.getTotpLastTimeStep());
    }

    @Test
    void lockedAccountRejectedOnTwoFactorPost() throws Exception {
        String secret = twoFactorService.generateSecret();
        User admin = persistUser("totp_test_locked", "AdminPass1234!", Role.ADMIN);
        String enrollCode = twoFactorService.generateCurrentCode(secret);
        twoFactorService.completeEnrollmentAndIssueRecoveryCodes(admin, secret, enrollCode);

        MvcResult login = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", admin.getUsername())
                        .param("password", "AdminPass1234!"))
                .andExpect(redirectedUrl("/2fa/verify"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();

        transactionTemplate.executeWithoutResult(status -> {
            User u = userRepository.findByUsername(admin.getUsername()).orElseThrow();
            u.setLockedUntil(LocalDateTime.now().plusHours(1));
            u.setFailedLoginAttempts(99);
            userRepository.save(u);
        });

        mockMvc.perform(post("/2fa/verify").session(session).with(csrf()).param("code", "000000"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void wrongTotpCodeRejected() throws Exception {
        String secret = twoFactorService.generateSecret();
        User admin = persistUser("totp_test_wrong", "AdminPass1234!", Role.ADMIN);
        String code = twoFactorService.generateCurrentCode(secret);
        twoFactorService.completeEnrollmentAndIssueRecoveryCodes(
                userRepository.findByUsername(admin.getUsername()).orElseThrow(), secret, code);

        MvcResult login = mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", admin.getUsername())
                        .param("password", "AdminPass1234!"))
                .andExpect(redirectedUrl("/2fa/verify"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) login.getRequest().getSession();
        mockMvc.perform(post("/2fa/verify").session(session).with(csrf()).param("code", "000000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/2fa/verify"));

        mockMvc.perform(get("/2fa/verify").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid authentication code")));
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
