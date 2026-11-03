package app.security;

public final class TwoFactorSessionKeys {

    private TwoFactorSessionKeys() {}

    /** Set after a successful password login (form); triggers the 2FA gate for privileged roles. */
    public static final String PASSWORD_LOGIN = "twoFactor.passwordLogin";

    /** True once TOTP (or recovery code) verification succeeded for this session. */
    public static final String VERIFIED = "twoFactor.verified";

    /** Plain TOTP secret while enrollment is in progress (never persisted until confirmed). */
    public static final String ENROLLMENT_SECRET = "twoFactor.enrollmentSecret";

    /** Failed TOTP/recovery attempts in the current pending-2FA session. */
    public static final String FAILED_ATTEMPTS = "twoFactor.failedAttempts";
}
