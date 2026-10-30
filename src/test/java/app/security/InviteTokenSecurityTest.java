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

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

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

    @Test
    void registrationWithMissingTokenCreatesNoUser() {
        long userCountBefore = userRepository.count();
        
        RegistrationDTO dto = new RegistrationDTO();
        dto.setUsername("notoken");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setEmail("notoken@test.com");
        dto.setCountry(Country.GERMANY);
        dto.setInviteToken(null);

        assertThrows(IllegalArgumentException.class, () -> {
            userService.register(dto);
        });

        assertEquals(userCountBefore, userRepository.count(), "No user should be created with missing token");
    }

    @Test
    void registrationWithInvalidTokenCreatesNoUser() {
        long userCountBefore = userRepository.count();
        
        RegistrationDTO dto = new RegistrationDTO();
        dto.setUsername("badtoken");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setEmail("badtoken@test.com");
        dto.setCountry(Country.GERMANY);
        dto.setInviteToken("forged-token-xyz");

        assertThrows(IllegalArgumentException.class, () -> {
            userService.register(dto);
        });

        assertEquals(userCountBefore, userRepository.count(), "No user should be created with invalid token");
    }

    @Test
    void registrationWithUsedTokenCreatesNoUser() {
        authenticateAs(adminUser);
        String token = inviteTokenService.createInviteToken();
        SecurityContextHolder.clearContext();

        // First registration succeeds
        RegistrationDTO dto1 = new RegistrationDTO();
        dto1.setUsername("first");
        dto1.setPassword("password123");
        dto1.setConfirmPassword("password123");
        dto1.setEmail("first@test.com");
        dto1.setCountry(Country.GERMANY);
        dto1.setInviteToken(token);
        userService.register(dto1);

        long userCountBefore = userRepository.count();

        // Second registration with same token fails
        RegistrationDTO dto2 = new RegistrationDTO();
        dto2.setUsername("second");
        dto2.setPassword("password123");
        dto2.setConfirmPassword("password123");
        dto2.setEmail("second@test.com");
        dto2.setCountry(Country.GERMANY);
        dto2.setInviteToken(token);

        assertThrows(IllegalArgumentException.class, () -> {
            userService.register(dto2);
        });

        assertEquals(userCountBefore, userRepository.count(), "No user should be created with used token");
    }

    @Test
    void registrationWithExpiredTokenCreatesNoUser() {
        authenticateAs(adminUser);
        String token = inviteTokenService.createInviteToken();

        // Manually expire the token
        String tokenHash = hashToken(token);
        InviteToken inviteToken = inviteTokenRepository.findByTokenHash(tokenHash).orElseThrow();
        inviteToken.setExpiresAt(LocalDateTime.now().minusDays(1));
        inviteTokenRepository.save(inviteToken);

        SecurityContextHolder.clearContext();
        long userCountBefore = userRepository.count();

        RegistrationDTO dto = new RegistrationDTO();
        dto.setUsername("expired");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");
        dto.setEmail("expired@test.com");
        dto.setCountry(Country.GERMANY);
        dto.setInviteToken(token);

        assertThrows(IllegalArgumentException.class, () -> {
            userService.register(dto);
        });

        assertEquals(userCountBefore, userRepository.count(), "No user should be created with expired token");
    }

    @Test
    @org.springframework.test.annotation.DirtiesContext
    void concurrentRegistrationWithSameTokenOnlyCreatesOneUser() throws Exception {
        authenticateAs(adminUser);
        String token = inviteTokenService.createInviteToken();
        entityManager.flush();
        org.springframework.test.context.transaction.TestTransaction.flagForCommit();
        org.springframework.test.context.transaction.TestTransaction.end();
        SecurityContextHolder.clearContext();
        
        // Token is now committed and visible to other threads
        long userCountBefore = userRepository.count();

        long timestamp = System.currentTimeMillis();
        RegistrationDTO dto1 = new RegistrationDTO();
        dto1.setUsername("concurrent1_" + timestamp);
        dto1.setPassword("password123");
        dto1.setConfirmPassword("password123");
        dto1.setEmail("concurrent1_" + timestamp + "@test.com");
        dto1.setCountry(Country.GERMANY);
        dto1.setInviteToken(token);

        RegistrationDTO dto2 = new RegistrationDTO();
        dto2.setUsername("concurrent2_" + timestamp);
        dto2.setPassword("password123");
        dto2.setConfirmPassword("password123");
        dto2.setEmail("concurrent2_" + timestamp + "@test.com");
        dto2.setCountry(Country.GERMANY);
        dto2.setInviteToken(token);

        // Execute registrations concurrently
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(2);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger failureCount = new java.util.concurrent.atomic.AtomicInteger(0);

        Thread thread1 = new Thread(() -> {
            try {
                startLatch.await();
                userService.register(dto1);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        Thread thread2 = new Thread(() -> {
            try {
                startLatch.await();
                userService.register(dto2);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        });

        thread1.start();
        thread2.start();
        Thread.sleep(100); // Give threads time to start
        startLatch.countDown(); // Start both threads simultaneously

        doneLatch.await(10, java.util.concurrent.TimeUnit.SECONDS);

        assertEquals(1, successCount.get(), "Exactly one registration should succeed");
        assertEquals(1, failureCount.get(), "Exactly one registration should fail");
        long userCountAfter = userRepository.count();
        assertEquals(userCountBefore + 1, userCountAfter, "Only one user should be created");
    }
}
