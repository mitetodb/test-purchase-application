package app.service;

import app.TestPurchaseApplication;
import app.model.entity.User;
import app.model.entity.UserTotpRecoveryCode;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.UserRepository;
import app.repository.UserTotpRecoveryCodeRepository;
import app.service.impl.TwoFactorServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static app.service.impl.TwoFactorServiceImpl.RECOVERY_INVALIDATION_REASON_REGENERATED;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = TestPurchaseApplication.class)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TwoFactorRecoveryCodeTest {

    @org.springframework.boot.test.mock.mockito.MockBean
    private app.client.PricingClient pricingClient;

    @Autowired
    private TwoFactorService twoFactorService;

    @Autowired
    private TwoFactorServiceImpl twoFactorServiceImpl;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTotpRecoveryCodeRepository recoveryCodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void deleteCreatedUsers() {
        transactionTemplate.executeWithoutResult(status ->
                userRepository.findAll().stream()
                        .filter(u -> u.getUsername().startsWith("recovery_test_"))
                        .forEach(u -> userRepository.deleteById(u.getId())));
    }

    @Test
    void regeneratedOldRecoveryCodeRejectedNewCodeWorksRowsStayInvalidated() {
        User admin = persistUser("recovery_test_admin", "AdminPass1234!", Role.ADMIN);
        String secret = twoFactorServiceImpl.generateSecret();
        String enrollCode = twoFactorServiceImpl.generateCurrentCode(secret);

        List<String> firstBatch = transactionTemplate.execute(status ->
                twoFactorService.completeEnrollmentAndIssueRecoveryCodes(
                        userRepository.findByUsername(admin.getUsername()).orElseThrow(), secret, enrollCode));
        assertNotNull(firstBatch);
        assertEquals(10, firstBatch.size());
        String oldPlainCode = firstBatch.get(0);

        List<String> secondBatch = transactionTemplate.execute(status -> {
            User reloaded = userRepository.findByUsername(admin.getUsername()).orElseThrow();
            return twoFactorService.regenerateRecoveryCodes(reloaded);
        });
        assertEquals(10, secondBatch.size());
        String newPlainCode = secondBatch.get(0);

        transactionTemplate.executeWithoutResult(status -> {
            User user = userRepository.findByUsername(admin.getUsername()).orElseThrow();
            assertFalse(twoFactorService.consumeRecoveryCode(user, oldPlainCode));
            assertTrue(twoFactorService.consumeRecoveryCode(user, newPlainCode));
        });

        List<UserTotpRecoveryCode> rows = recoveryCodeRepository.findByUser_Id(admin.getId());
        assertEquals(20, rows.size());
        List<UserTotpRecoveryCode> invalidated = rows.stream()
                .filter(c -> c.getInvalidatedAt() != null)
                .toList();
        assertEquals(10, invalidated.size());
        assertTrue(invalidated.stream()
                .allMatch(c -> RECOVERY_INVALIDATION_REASON_REGENERATED.equals(c.getInvalidationReason())));
        assertTrue(invalidated.stream().allMatch(c -> c.getUsedAt() == null));

        long consumableRemaining = rows.stream()
                .filter(c -> c.getUsedAt() == null && c.getInvalidatedAt() == null)
                .count();
        assertEquals(9, consumableRemaining);
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
