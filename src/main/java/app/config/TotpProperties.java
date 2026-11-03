package app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.totp")
public record TotpProperties(String encryptionKey, String issuer) {
    public TotpProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "Test Purchase";
        }
    }
}
