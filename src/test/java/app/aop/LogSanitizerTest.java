package app.aop;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    @Test
    void masksSensitiveNamedArguments() {
        String out = LogSanitizer.sanitizeArgs(new String[]{"token", "newPassword", "email"},
                new Object[]{"abc123secretvalue", "hunter2", "a@b.c"});
        assertThat(out).isEqualTo("[***, ***, a@b.c]").doesNotContain("abc123secretvalue").doesNotContain("hunter2");
    }

    @Test
    void masksPairsInsideToStringAndUrls() {
        String dto = "ResetPasswordDTO(token=tok-XYZ, password=Sup3r, confirmPassword=Sup3r, email=x@y.z)";
        String out = LogSanitizer.sanitize(dto);
        assertThat(out).doesNotContain("tok-XYZ").doesNotContain("Sup3r").contains("email=x@y.z").contains("token=***");
        assertThat(LogSanitizer.sanitize("redirect:https://h/reset-password?token=abc&lang=bg"))
                .isEqualTo("redirect:https://h/reset-password?token=***&lang=bg");
        assertThat(LogSanitizer.sanitize("X-API-Key: k123")).doesNotContain("k123");
    }

    @Test
    void leavesOrdinaryValuesAlone() {
        assertThat(LogSanitizer.sanitize("auth/forgot-password")).isEqualTo("auth/forgot-password");
        assertThat(LogSanitizer.sanitize(null)).isEqualTo("null");
    }
}
