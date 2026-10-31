package app.rendering;

import app.TestPurchaseApplication;
import app.model.entity.InviteToken;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.CustomerRepository;
import app.repository.InviteTokenRepository;
import app.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for registration page rendering with and without invitation tokens.
 * NOT @Transactional to catch lazy-loading issues.
 */
@SpringBootTest(classes = TestPurchaseApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private InviteTokenRepository inviteTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private String validToken;

    @BeforeEach
    void setup() throws Exception {
        String uuid = Integer.toHexString((int)(Math.random() * 65536)); // 4-char hex
        
        // Create admin user with unique email
        adminUser = User.builder()
                .username("admin_registration_" + uuid)
                .email("admin_registration_" + uuid + "@test.com")
                .password(passwordEncoder.encode("password"))
                .role(Role.ADMIN)
                .active(true)
                .country(Country.GERMANY)
                .managedCustomers(new HashSet<>())
                .build();
        adminUser = userRepository.save(adminUser);

        // Create a valid invite token
        validToken = createValidInviteToken();
    }

    private String createValidInviteToken() throws Exception {
        // Generate a random token
        byte[] tokenBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        // Hash the token for storage
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
        String tokenHash = bytesToHex(hash);

        InviteToken inviteToken = InviteToken.builder()
                .tokenHash(tokenHash)
                .createdBy(adminUser)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        inviteTokenRepository.save(inviteToken);
        return rawToken;
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Test
    void registerWithoutTokenShowsInvitationRequired() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(content().string(containsString("Invitation Required")))
                .andExpect(content().string(not(containsString("<form"))));
    }

    @Test
    void registerWithEmptyTokenShowsInvitationRequired() throws Exception {
        mockMvc.perform(get("/register").param("token", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(content().string(containsString("Invitation Required")))
                .andExpect(content().string(not(containsString("<form"))));
    }

    @Test
    void registerWithInvalidTokenShowsForm() throws Exception {
        // Invalid token should show the form (validation happens on POST)
        mockMvc.perform(get("/register").param("token", "invalid-token-12345"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(content().string(not(containsString("Invitation Required"))))
                .andExpect(content().string(containsString("<form")));
    }

    @Test
    void registerWithValidTokenShowsForm() throws Exception {
        mockMvc.perform(get("/register").param("token", validToken))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(content().string(not(containsString("Invitation Required"))))
                .andExpect(content().string(containsString("<form")))
                .andExpect(content().string(containsString("Create Account")));
    }

    @Test
    void registerPageDoesNotShowErrorPage() throws Exception {
        mockMvc.perform(get("/register").param("token", validToken))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("error-page"))));
    }
}
