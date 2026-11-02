package app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.login")
public record LoginSecurityProperties(
        int maxFailedAttempts,
        int lockoutDurationMinutes,
        RateLimit rateLimit
) {
    public LoginSecurityProperties {
        if (maxFailedAttempts <= 0) {
            maxFailedAttempts = 5;
        }
        if (lockoutDurationMinutes <= 0) {
            lockoutDurationMinutes = 15;
        }
        if (rateLimit == null) {
            rateLimit = new RateLimit(20, 60);
        }
    }

    public record RateLimit(int maxAttempts, int windowSeconds) {
        public RateLimit {
            if (maxAttempts <= 0) {
                maxAttempts = 20;
            }
            if (windowSeconds <= 0) {
                windowSeconds = 60;
            }
        }
    }
}
