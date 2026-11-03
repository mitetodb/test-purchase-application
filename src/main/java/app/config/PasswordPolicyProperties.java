package app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.password")
public record PasswordPolicyProperties(int minLength) {
    public PasswordPolicyProperties {
        if (minLength <= 0) {
            minLength = 12;
        }
    }
}
