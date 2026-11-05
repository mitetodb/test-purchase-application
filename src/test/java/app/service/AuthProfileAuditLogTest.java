package app.service;

import app.TestPurchaseApplication;
import app.model.dto.RegistrationDTO;
import app.model.dto.UserProfileDTO;
import app.model.entity.User;
import app.model.enums.AuditAction;
import app.model.enums.Country;
import app.model.enums.PasswordChangeKind;
import app.model.enums.Role;
import app.repository.AuditLogRepository;
import app.repository.InviteTokenRepository;
import app.repository.UserTotpRecoveryCodeRepository;
import app.repository.UserRepository;
import app.service.impl.TwoFactorServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AuthProfileAuditLogTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    @Autowired
    private UserService userService;

    @Autowired
    private TwoFactorService twoFactorService;

    @Autowired
    private TwoFactorServiceImpl twoFactorServiceImpl;

    @Autowired
    private InviteTokenService inviteTokenService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private InviteTokenRepository inviteTokenRepository;

    @Autowired
    private UserTotpRecoveryCodeRepository recoveryCodeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void cleanupUsers() {
        SecurityContextHolder.clearContext();
        transactionTemplate.executeWithoutResult(status -> {
            userRepository.findAll().stream()
                    .filter(u -> u.getUsername().startsWith("auth_audit_"))
                    .forEach(u -> {
                        inviteTokenRepository.findAll().stream()
                                .filter(t -> (t.getCreatedBy() != null && u.getId().equals(t.getCreatedBy().getId()))
                                        || (t.getUsedBy() != null && u.getId().equals(t.getUsedBy().getId())))
                                .forEach(inviteTokenRepository::delete);
                        recoveryCodeRepository.findByUser_Id(u.getId())
                                .forEach(recoveryCodeRepository::delete);
                        userRepository.deleteById(u.getId());
                    });
        });
    }

    @Test
    void profileUpdateWritesAuditEntry() {
        User user = persistUser("auth_audit_profile", Role.MYSTERY_SHOPPER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), null, List.of()));

        UUID userId = user.getId();
        long before = countAudit(AuditAction.PROFILE_UPDATE, userId);

        transactionTemplate.executeWithoutResult(status -> {
            UserProfileDTO dto = new UserProfileDTO();
            dto.setEmail("updated@example.com");
            dto.setCountry(Country.GERMANY);
            userService.updateProfile(user.getUsername(), dto, "/uploads/new-avatar.png");
        });

        assertEquals(before + 1, countAudit(AuditAction.PROFILE_UPDATE, userId));
        assertTrue(latestSummary(AuditAction.PROFILE_UPDATE, userId).contains("email:"));
    }

    @Test
    void passwordChangeWritesAuditEntry() {
        User user = persistUser("auth_audit_pw", Role.MYSTERY_SHOPPER);
        UUID userId = user.getId();
        long before = countAudit(AuditAction.PASSWORD_CHANGE, userId);

        transactionTemplate.executeWithoutResult(status ->
                userService.changePassword(
                        userRepository.findById(userId).orElseThrow(),
                        "NewPassword123!",
                        PasswordChangeKind.PASSWORD_RESET));

        assertEquals(before + 1, countAudit(AuditAction.PASSWORD_CHANGE, userId));
        assertTrue(latestSummary(AuditAction.PASSWORD_CHANGE, userId).contains("source=PASSWORD_RESET"));
    }

    @Test
    void inviteRegistrationWritesPasswordChangeAudit() {
        User admin = persistUser("auth_audit_inv_admin", Role.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        admin.getUsername(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        String token = inviteTokenService.createInviteToken();
        SecurityContextHolder.clearContext();

        RegistrationDTO dto = new RegistrationDTO();
        dto.setUsername("auth_audit_reg");
        dto.setPassword("RegPassword123!");
        dto.setConfirmPassword("RegPassword123!");
        dto.setEmail("auth_audit_reg@example.com");
        dto.setCountry(Country.BULGARIA);
        dto.setInviteToken(token);

        User registered = transactionTemplate.execute(status -> userService.register(dto));
        assertNotNull(registered);

        assertTrue(auditLogRepository.findAll().stream()
                .anyMatch(row -> row.getAction() == AuditAction.PASSWORD_CHANGE
                        && registered.getId().toString().equals(row.getEntityId())
                        && row.getChangeSummary().contains("INVITE_REGISTRATION")));
    }

    @Test
    void totpEnrollVerifyRecoveryAndRegenerateWriteAuditEntries() {
        User admin = persistUser("auth_audit_totp", Role.ADMIN);
        UUID userId = admin.getId();
        String secret = twoFactorServiceImpl.generateSecret();
        long step = twoFactorServiceImpl.currentTimeStep();
        String enrollCode = twoFactorServiceImpl.generateCodeForTimeStep(secret, step);

        transactionTemplate.executeWithoutResult(status -> {
            User fresh = userRepository.findById(userId).orElseThrow();
            twoFactorService.completeEnrollmentAndIssueRecoveryCodes(fresh, secret, enrollCode);
        });

        assertTrue(hasAudit(AuditAction.TOTP_ENROLL, userId));
        assertFalse(hasAudit(AuditAction.TOTP_RECOVERY_REGENERATE, userId));

        transactionTemplate.executeWithoutResult(status -> {
            User reloaded = userRepository.findById(userId).orElseThrow();
            assertFalse(twoFactorService.verifyTotpCode(reloaded, "000000"));
        });
        assertTrue(hasAudit(AuditAction.TOTP_VERIFY_FAILURE, userId));

        String validCode = twoFactorServiceImpl.generateCodeForTimeStep(secret, step + 1);
        transactionTemplate.executeWithoutResult(status -> {
            User reloaded = userRepository.findById(userId).orElseThrow();
            assertTrue(twoFactorService.verifyTotpCode(reloaded, validCode));
        });
        assertTrue(hasAudit(AuditAction.TOTP_VERIFY_SUCCESS, userId));

        List<String> codes = transactionTemplate.execute(status -> {
            User reloaded = userRepository.findById(userId).orElseThrow();
            return twoFactorService.regenerateRecoveryCodes(reloaded);
        });
        assertNotNull(codes);
        assertTrue(hasAudit(AuditAction.TOTP_RECOVERY_REGENERATE, userId));

        String recoveryPlain = codes.get(1);
        transactionTemplate.executeWithoutResult(status -> {
            User reloaded = userRepository.findById(userId).orElseThrow();
            assertTrue(twoFactorService.consumeRecoveryCode(reloaded, recoveryPlain));
        });
        assertTrue(hasAudit(AuditAction.TOTP_RECOVERY_USE, userId));
    }

    private User persistUser(String username, Role role) {
        return transactionTemplate.execute(status -> {
            userRepository.findByUsername(username).ifPresent(u -> userRepository.deleteById(u.getId()));
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode("StartPassword12!"));
            user.setEmail(username + "@example.com");
            user.setRole(role);
            user.setActive(true);
            user.setCountry(Country.BULGARIA);
            return userRepository.save(user);
        });
    }

    private long countAudit(AuditAction action, UUID userId) {
        return auditLogRepository.findAll().stream()
                .filter(row -> row.getAction() == action && userId.toString().equals(row.getEntityId()))
                .count();
    }

    private boolean hasAudit(AuditAction action, UUID userId) {
        return countAudit(action, userId) > 0;
    }

    private String latestSummary(AuditAction action, UUID userId) {
        return auditLogRepository.findAll().stream()
                .filter(row -> row.getAction() == action && userId.toString().equals(row.getEntityId()))
                .reduce((first, second) -> second)
                .map(row -> row.getChangeSummary())
                .orElse("");
    }
}
