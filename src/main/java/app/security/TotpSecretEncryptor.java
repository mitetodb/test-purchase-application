package app.security;

import app.config.TotpProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class TotpSecretEncryptor {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final TotpProperties properties;
    private SecretKey secretKey;

    public TotpSecretEncryptor(TotpProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        String keyMaterial = properties.encryptionKey();
        if (keyMaterial == null || keyMaterial.isBlank()) {
            throw new IllegalStateException(
                    "APP_TOTP_ENCRYPTION_KEY must be set (Base64-encoded 32-byte AES key)");
        }
        byte[] decoded = Base64.getDecoder().decode(keyMaterial.trim());
        if (decoded.length != 32) {
            throw new IllegalStateException("APP_TOTP_ENCRYPTION_KEY must decode to 32 bytes");
        }
        this.secretKey = new SecretKeySpec(decoded, "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("TOTP secret encryption failed", ex);
        }
    }

    public String decrypt(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(stored);
            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] plain = cipher.doFinal(ciphertext);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("TOTP secret decryption failed", ex);
        }
    }
}
