package app.service;

import app.config.PasswordPolicyProperties;
import app.service.impl.PasswordPolicyServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyServiceTest {

    private final PasswordPolicyService service =
            new PasswordPolicyServiceImpl(new PasswordPolicyProperties(12));

    @Test
    void acceptsPasswordAtMinLength() {
        assertThat(service.isValid("123456789012")).isTrue();
    }

    @Test
    void rejectsShorterPassword() {
        assertThat(service.isValid("12345678901")).isFalse();
    }

    @Test
    void validateOrThrowUsesMessageKey() {
        assertThatThrownBy(() -> service.validateOrThrow("short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("password.policy.tooShort");
    }
}
