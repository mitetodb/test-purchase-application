package app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.two-factor")
public record TwoFactorSecurityProperties(int maxSessionFailedAttempts) {
    public TwoFactorSecurityProperties {
        if (maxSessionFailedAttempts <= 0) {
            maxSessionFailedAttempts = 5;
        }
    }
}
