package app.audit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditSummarySanitizerTest {

    @Test
    void masksPasswordAndTokenFields() {
        String raw = "username=demo; password=SuperSecret123!; inviteToken=abc123tokenvalue";
        String sanitized = AuditSummarySanitizer.sanitize(raw);
        assertFalse(sanitized.contains("SuperSecret123!"));
        assertFalse(sanitized.contains("abc123tokenvalue"));
        assertTrue(sanitized.contains("password=" + "***"));
        assertTrue(sanitized.contains("inviteToken=" + "***"));
    }

    @Test
    void masksTotpSecretPairs() {
        String sanitized = AuditSummarySanitizer.sanitize("totpSecret=JBSWY3DPEHPK3PXP");
        assertFalse(sanitized.contains("JBSWY3DPEHPK3PXP"));
    }
}
