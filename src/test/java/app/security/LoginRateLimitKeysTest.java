package app.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginRateLimitKeysTest {

    @Test
    void storageKeyIsSha256Hex() {
        String key = LoginRateLimitKeys.storageKey("user:", "abc");
        assertEquals(64, key.length());
        assertEquals(key, LoginRateLimitKeys.storageKey("user:", "abc"));
    }

    @Test
    void longUsernameProducesFixedLengthKey() {
        String longName = "u".repeat(1000);
        String key = LoginRateLimitKeys.storageKey("user:", longName.toLowerCase());
        assertEquals(64, key.length());
    }
}
