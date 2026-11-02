package app.support;

import app.repository.PasswordResetTokenRepository;
import app.repository.UserRepository;
import app.repository.UserTotpRecoveryCodeRepository;

/**
 * Deletes users and dependent rows in FK-safe order for isolated integration tests.
 */
public final class TestDatabaseCleanup {

    private TestDatabaseCleanup() {}

    public static void deleteAllUsersAndDependents(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            UserTotpRecoveryCodeRepository userTotpRecoveryCodeRepository) {
        passwordResetTokenRepository.deleteAll();
        userTotpRecoveryCodeRepository.deleteAll();
        userRepository.deleteAll();
    }
}
