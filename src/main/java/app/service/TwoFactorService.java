package app.service;

import app.model.entity.User;

import java.util.List;

public interface TwoFactorService {

    boolean requiresTwoFactor(User user);

    boolean requiresTwoFactor(app.model.enums.Role role);

    String generateSecret();

    String buildOtpAuthUri(String username, String secret);

    String buildQrCodeDataUri(String username, String secret);

    boolean verifyTotpCode(User user, String code);

    boolean verifyTotpCode(String plainSecret, String code, User userForReplay);

    void completeEnrollment(User user, String plainSecret, String confirmationCode);

    List<String> completeEnrollmentAndIssueRecoveryCodes(User user, String plainSecret, String confirmationCode);

    List<String> regenerateRecoveryCodes(User user);

    boolean consumeRecoveryCode(User user, String rawCode);

    long currentTimeStep();

    /** For integration tests with a enrolled user. */
    String generateCurrentCodeForUser(User user);

    /** Generates the TOTP code for an explicit time step (tests). */
    String generateCodeForTimeStep(String plainSecret, long timeStep);

    String generateCodeForUserAtTimeStep(User user, long timeStep);
}
