package app.service;

import app.config.ContactProperties;
import app.model.entity.LoginRateLimit;
import app.repository.LoginRateLimitRepository;
import app.repository.LoginRateLimitUpsert;
import app.security.LoginRateLimitKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactRateLimitService {

    private static final String IP_PREFIX = "contact-ip:";

    private final ContactProperties contactProperties;
    private final LoginRateLimitRepository rateLimitRepository;
    private final LoginRateLimitUpsert loginRateLimitUpsert;

    @Transactional(readOnly = true)
    public boolean isRateLimitExceeded(String clientIp) {
        return isBucketExceeded(normalize(clientIp));
    }

    @Transactional
    public void recordSubmission(String clientIp) {
        incrementBucket(normalize(clientIp));
    }

    private boolean isBucketExceeded(String normalizedIp) {
        String rateKey = LoginRateLimitKeys.storageKey(IP_PREFIX, normalizedIp);
        Optional<LoginRateLimit> bucket = rateLimitRepository.findByRateKey(rateKey);
        if (bucket.isEmpty()) {
            return false;
        }
        LoginRateLimit entry = bucket.get();
        if (windowExpired(entry)) {
            return false;
        }
        return entry.getAttemptCount() >= contactProperties.rateLimit().maxAttempts();
    }

    private void incrementBucket(String normalizedIp) {
        String rateKey = LoginRateLimitKeys.storageKey(IP_PREFIX, normalizedIp);
        LocalDateTime now = LocalDateTime.now();
        int windowSeconds = contactProperties.rateLimit().windowSeconds();
        LocalDateTime windowNotBefore = now.minusSeconds(windowSeconds);
        loginRateLimitUpsert.upsertIncrement(UUID.randomUUID(), rateKey, now, windowNotBefore);
    }

    private boolean windowExpired(LoginRateLimit entry) {
        return entry.getWindowStart()
                .plusSeconds(contactProperties.rateLimit().windowSeconds())
                .isBefore(LocalDateTime.now());
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "_empty_";
        }
        return value.trim().toLowerCase();
    }
}
