package app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.contact")
public record ContactProperties(
        String recipient,
        RateLimit rateLimit,
        long minSubmitDelayMs
) {

    public ContactProperties {
        if (recipient == null || recipient.isBlank()) {
            recipient = "mail@getinweb.de";
        }
        if (rateLimit == null) {
            rateLimit = new RateLimit(10, 3600);
        }
        if (minSubmitDelayMs <= 0) {
            minSubmitDelayMs = 3000L;
        }
    }

    public record RateLimit(int maxAttempts, int windowSeconds) {
        public RateLimit {
            if (maxAttempts <= 0) {
                maxAttempts = 10;
            }
            if (windowSeconds <= 0) {
                windowSeconds = 3600;
            }
        }
    }
}
