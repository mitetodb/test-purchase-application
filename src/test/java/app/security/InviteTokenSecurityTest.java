package app.security;

import app.TestPurchaseApplication;
import app.model.dto.RegistrationDTO;
import app.model.entity.InviteToken;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.InviteTokenRepository;
import app.repository.UserRepository;
import app.service.InviteTokenService;
import app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for invite-only registration system.
 */
@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
@Transactional
class InviteTokenSecurityTest {

    @Autowired
    private InviteTokenService inviteTokenService;

    @Autowired
    private UserService userService;

    @Autowired
    private InviteTokenRepository inviteTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private User accountManagerUser;
    private User salesManagerUser;

    @BeforeEach
    void setUp() {
        inviteTokenRepository.deleteAll();
        userRepository.deleteAll();

        // Create admin
        adminUser = new User();
        adminUser.setUsername("admin_invite");
        adminUser.setPassword("password");
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(Role.ADMIN);
        adminUser.setActive(true);
        adminUser = userRepository.save(adminUser);

        // Create account manager
        accountManagerUser = new User();
        accountManagerUser.setUsername("am_invite");
        accountManagerUser.setPassword("password");
        accountManagerUser.setEmail("am@test.com");
        accountManagerUser.setRole(Role.ACCOUNT_MANAGER);
        accountManagerUser.setActive(true);
        accountManagerUser = userRepository.save(accountManagerUser);

        // Create sales manager
        salesManagerUser = new User();
        salesManagerUser.setUsername("sm_invite");
        salesManagerUser.setPassword("password");
        salesManagerUser.setEmail("sm@test.com");
        salesManagerUser.setRole(Role.SALES_MANAGER);
        salesManagerUser.setActive(true);
        salesManagerUser = userRepository.save(salesManagerUser);
    }

    @Test
    void adminCanCreateInviteToken() {
        authenticateAs(adminUser);

        String token = inviteTokenService.createInviteToken();

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void accountManagerCanCreateInviteToken() {
        authenticateAs(accountManagerUser);

        String token = inviteTokenService.createInviteToken();

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void salesManagerCannotCreateInviteToken() {
        authenticateAs(salesManagerUser);

        assertThrows(AccessDeniedException.class, () -> {
            inviteTokenService.createInviteToken();
        }, "Sales Manager should not be able to create invite tokens");
    }

    @Test
    void registrationWithoutTokenFails() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setUsername("newuser");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setEmail("newuser@test.com");
        dto.setCountry(Country.GERMANY);
        dto.setInviteToken(null);

        assertThrows(IllegalArgumentException.class, () -> {
            userService.register(dto);
        }, "Registration without invite token should fail");
    }

    @Test
    void registrationWithInvalidTokenFails() {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setUsername("newuser");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setEmail("newuser@test.com");
        dto.setCountry(Country.GERMANY);
        dto.setInviteToken("invalid-token-12345");

        assertThrows(IllegalArgumentException.class, () -> {
            userService.register(dto);
        }, "Registration with invalid token should fail");
    }

    @Test
    void registrationWithValidTokenSucceeds() {
        authenticateAs(adminUser);
        String token = inviteTokenService.createInviteToken();

        SecurityContextHolder.clearContext();

        RegistrationDTO dto = new RegistrationDTO();
        dto.setUsername("newuser");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setEmail("newuser@test.com");
        dto.setCountry(Country.GERMANY);
        dto.setInviteToken(token);

        User registeredUser = userService.register(dto);

        assertNotNull(registeredUser);
        assertEquals(Role.MYSTERY_SHOPPER, registeredUser.getRole());
        assertEquals("newuser", registeredUser.getUsername());
    }

    @Test
    void registrationWithUsedTokenFails() {
        authenticateAs(adminUser);
        String token = inviteTokenService.createInviteToken();

        SecurityContextHolder.clearContext();

        // First registration
        RegistrationDTO dto1 = new RegistrationDTO();
        dto1.setUsername("user1");
        dto1.setPassword("password123");
        dto1.setConfirmPassword("password123");
        dto1.setEmail("user1@test.com");
        dto1.setCountry(Country.GERMANY);
        dto1.setInviteToken(token);
        userService.register(dto1);

        // Second registration with same token
        RegistrationDTO dto2 = new RegistrationDTO();
        dto2.setUsername("user2");
        dto2.setPassword("password123");
        dto2.setConfirmPassword("password123");
        dto2.setEmail("user2@test.com");
        dto2.setCountry(Country.GERMANY);
        dto2.setInviteToken(token);

        assertThrows(IllegalArgumentException.class, () -> {
            userService.register(dto2);
        }, "Registration with used token should fail");
    }

    @Test
    void registrationWithExpiredTokenFails() {
        authenticateAs(adminUser);
        String token = inviteTokenService.createInviteToken();

        // Manually expire the token
        String tokenHash = hashToken(token);
        InviteToken inviteToken = inviteTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        inviteToken.setExpiresAt(LocalDateTime.now().minusDays(1));
        inviteTokenRepository.save(inviteToken);

        SecurityContextHolder.clearContext();

        RegistrationDTO dto = new RegistrationDTO();
        dto.setUsername("newuser");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setEmail("newuser@test.com");
        dto.setCountry(Country.GERMANY);
        dto.setInviteToken(token);

        assertThrows(IllegalArgumentException.class, () -> {
            userService.register(dto);
        }, "Registration with expired token should fail");
    }

    @Test
    void registrationAlwaysAssignsMysteryShopperRole() {
        authenticateAs(adminUser);
        String token = inviteTokenService.createInviteToken();

        SecurityContextHolder.clearContext();

        RegistrationDTO dto = new RegistrationDTO();
        dto.setUsername("newuser");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setEmail("newuser@test.com");
        dto.setCountry(Country.GERMANY);
        dto.setInviteToken(token);

        User registeredUser = userService.register(dto);

        assertEquals(Role.MYSTERY_SHOPPER, registeredUser.getRole(), 
                "Public registration must always assign MYSTERY_SHOPPER role");
    }

    private void authenticateAs(User user) {
        String rolePrefix = "ROLE_" + user.getRole().name();
        var auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority(rolePrefix))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private String hashToken(String rawToken) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
