package app.service.impl;

import app.config.TotpProperties;
import app.model.entity.User;
import app.model.entity.UserTotpRecoveryCode;
import app.model.enums.Role;
import app.repository.UserRepository;
import app.repository.UserTotpRecoveryCodeRepository;
import app.security.TotpSecretEncryptor;
import app.service.TwoFactorService;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TwoFactorServiceImpl implements TwoFactorService {

    private static final Set<Role> ROLES_REQUIRING_2FA = EnumSet.of(Role.ADMIN, Role.ACCOUNT_MANAGER);
    private static final int RECOVERY_CODE_COUNT = 10;
    private static final int RECOVERY_CODE_BYTES = 5;
    public static final String RECOVERY_INVALIDATION_REASON_REGENERATED = "REGENERATED";

    private final TotpProperties totpProperties;
    private final TotpSecretEncryptor encryptor;
    private final UserRepository userRepository;
    private final UserTotpRecoveryCodeRepository recoveryCodeRepository;
    private final PasswordEncoder passwordEncoder;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final DefaultCodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();

    @Override
    public boolean requiresTwoFactor(User user) {
        return user != null && requiresTwoFactor(user.getRole());
    }

    @Override
    public boolean requiresTwoFactor(Role role) {
        return role != null && ROLES_REQUIRING_2FA.contains(role);
    }

    @Override
    public String generateSecret() {
        return secretGenerator.generate();
    }

    @Override
    public String buildOtpAuthUri(String username, String secret) {
        String issuer = URLEncoder.encode(totpProperties.issuer(), StandardCharsets.UTF_8);
        String account = URLEncoder.encode(username, StandardCharsets.UTF_8);
        return "otpauth://totp/" + issuer + ":" + account + "?secret=" + secret + "&issuer=" + issuer
                + "&algorithm=SHA1&digits=6&period=30";
    }

    @Override
    public String buildQrCodeDataUri(String username, String secret) {
        QrData data = new QrData.Builder()
                .label(username)
                .secret(secret)
                .issuer(totpProperties.issuer())
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        try {
            byte[] image = qrGenerator.generate(data);
            return "data:" + qrGenerator.getImageMimeType() + ";base64,"
                    + java.util.Base64.getEncoder().encodeToString(image);
        } catch (QrGenerationException ex) {
            throw new IllegalStateException("QR generation failed", ex);
        }
    }

    @Override
    @Transactional
    public boolean verifyTotpCode(User user, String code) {
        if (user == null || !user.isTotpEnabled() || user.getTotpSecretEncrypted() == null) {
            return false;
        }
        String secret = encryptor.decrypt(user.getTotpSecretEncrypted());
        return verifyTotpCode(secret, code, user);
    }

    @Override
    @Transactional
    public boolean verifyTotpCode(String plainSecret, String code, User userForReplay) {
        if (plainSecret == null || code == null || code.isBlank()) {
            return false;
        }
        String normalized = code.replaceAll("\\s+", "");
        Long matchedStep = findMatchedTimeStep(plainSecret, normalized);
        if (matchedStep == null) {
            return false;
        }
        if (userForReplay != null && userForReplay.getTotpLastTimeStep() != null
                && matchedStep <= userForReplay.getTotpLastTimeStep()) {
            return false;
        }
        if (userForReplay != null) {
            userForReplay.setTotpLastTimeStep(matchedStep);
            userRepository.save(userForReplay);
        }
        return true;
    }

    @Override
    @Transactional
    public void completeEnrollment(User user, String plainSecret, String confirmationCode) {
        completeEnrollmentAndIssueRecoveryCodes(user, plainSecret, confirmationCode);
    }

    @Override
    @Transactional
    public List<String> completeEnrollmentAndIssueRecoveryCodes(
            User user, String plainSecret, String confirmationCode) {
        String normalized = confirmationCode.replaceAll("\\s+", "");
        Long matchedStep = findMatchedTimeStep(plainSecret, normalized);
        if (matchedStep == null) {
            throw new IllegalArgumentException("twoFactor.error.invalidCode");
        }
        user.setTotpSecretEncrypted(encryptor.encrypt(plainSecret));
        user.setTotpEnabled(true);
        user.setTotpLastTimeStep(matchedStep);
        userRepository.save(user);
        return regenerateRecoveryCodes(user);
    }

    @Override
    @Transactional
    public List<String> regenerateRecoveryCodes(User user) {
        LocalDateTime now = LocalDateTime.now();
        List<UserTotpRecoveryCode> active = recoveryCodeRepository.findConsumableByUserId(user.getId());
        for (UserTotpRecoveryCode existing : active) {
            existing.setInvalidatedAt(now);
            existing.setInvalidationReason(RECOVERY_INVALIDATION_REASON_REGENERATED);
        }
        recoveryCodeRepository.saveAll(active);

        List<String> plainCodes = new ArrayList<>(RECOVERY_CODE_COUNT);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            String plain = formatRecoveryCode(random);
            plainCodes.add(plain);
            recoveryCodeRepository.save(UserTotpRecoveryCode.builder()
                    .user(user)
                    .codeHash(passwordEncoder.encode(normalizeRecoveryCode(plain)))
                    .build());
        }
        return plainCodes;
    }

    @Override
    @Transactional
    public boolean consumeRecoveryCode(User user, String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            return false;
        }
        String normalized = normalizeRecoveryCode(rawCode);
        List<UserTotpRecoveryCode> consumable = recoveryCodeRepository.findConsumableByUserId(user.getId());
        for (UserTotpRecoveryCode stored : consumable) {
            if (passwordEncoder.matches(normalized, stored.getCodeHash())) {
                stored.setUsedAt(LocalDateTime.now());
                recoveryCodeRepository.save(stored);
                return true;
            }
        }
        return false;
    }

    @Override
    public long currentTimeStep() {
        return timeProvider.getTime() / 30;
    }

    @Override
    public String generateCurrentCodeForUser(User user) {
        if (user == null || user.getTotpSecretEncrypted() == null) {
            throw new IllegalStateException("User has no TOTP secret");
        }
        return generateCurrentCode(encryptor.decrypt(user.getTotpSecretEncrypted()));
    }

    /** For tests: generate the current TOTP code for a secret. */
    public String generateCurrentCode(String plainSecret) {
        return generateCodeForTimeStep(plainSecret, currentTimeStep());
    }

    @Override
    public String generateCodeForTimeStep(String plainSecret, long timeStep) {
        try {
            return codeGenerator.generate(plainSecret, Math.toIntExact(timeStep));
        } catch (CodeGenerationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public String generateCodeForUserAtTimeStep(User user, long timeStep) {
        if (user == null || user.getTotpSecretEncrypted() == null) {
            throw new IllegalStateException("User has no TOTP secret");
        }
        return generateCodeForTimeStep(encryptor.decrypt(user.getTotpSecretEncrypted()), timeStep);
    }

    private static String formatRecoveryCode(SecureRandom random) {
        byte[] bytes = new byte[RECOVERY_CODE_BYTES];
        random.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i == RECOVERY_CODE_BYTES / 2) {
                sb.append('-');
            }
            sb.append(String.format("%02x", bytes[i]));
        }
        return sb.toString();
    }

    private static String normalizeRecoveryCode(String raw) {
        return raw.trim().toLowerCase().replace(" ", "");
    }

    /**
     * Resolves which 30s TOTP window matched the code (same window as {@link DefaultCodeVerifier}).
     */
    private Long findMatchedTimeStep(String plainSecret, String normalizedCode) {
        if (!codeVerifier.isValidCode(plainSecret, normalizedCode)) {
            return null;
        }
        long current = currentTimeStep();
        int discrepancy = 1;
        for (long step = current - discrepancy; step <= current + discrepancy; step++) {
            try {
                String expected = codeGenerator.generate(plainSecret, Math.toIntExact(step));
                if (expected.equals(normalizedCode)) {
                    return step;
                }
            } catch (CodeGenerationException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return null;
    }
}
