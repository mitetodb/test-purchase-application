package app.service.impl;

import app.config.LoginSecurityProperties;
import app.model.entity.LoginRateLimit;
import app.model.entity.User;
import app.repository.LoginRateLimitRepository;
import app.repository.LoginRateLimitUpsert;
import app.repository.UserRepository;
import app.security.LoginRateLimitKeys;
import app.service.LoginSecurityService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginSecurityServiceImpl implements LoginSecurityService {

    private static final String IP_PREFIX = "ip:";
    private static final String USER_PREFIX = "user:";

    private final LoginSecurityProperties properties;
    private final LoginRateLimitRepository rateLimitRepository;
    private final LoginRateLimitUpsert loginRateLimitUpsert;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isRateLimitExceeded(String clientIp, String username) {
        return isBucketExceeded(IP_PREFIX, normalize(clientIp))
                || isBucketExceeded(USER_PREFIX, normalize(username));
    }

    @Override
    @Transactional
    public void recordLoginAttempt(String clientIp, String username) {
        incrementBucket(IP_PREFIX, normalize(clientIp));
        incrementBucket(USER_PREFIX, normalize(username));
    }

    @Override
    @Transactional
    public void handleFailedLogin(String clientIp, String username) {
        recordLoginAttempt(clientIp, username);

        if (username == null || username.isBlank()) {
            return;
        }

        userRepository.findByUsername(username).ifPresent(user -> {
            if (isCurrentlyLocked(user)) {
                return;
            }
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= properties.maxFailedAttempts()) {
                LocalDateTime lockedUntil = LocalDateTime.now()
                        .plusMinutes(properties.lockoutDurationMinutes());
                user.setLockedUntil(lockedUntil);
                log.info("Account locked: username={}, lockedUntil={}", user.getUsername(), lockedUntil);
            }
            userRepository.save(user);
        });
    }

    @Override
    @Transactional
    public void handleSuccessfulLogin(String username) {
        if (username == null || username.isBlank()) {
            return;
        }
        userRepository.findByUsername(username).ifPresent(user -> {
            if (user.getFailedLoginAttempts() != 0 || user.getLockedUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
        });
    }

    @Override
    @Transactional
    public void unlockAccount(UUID userId, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        log.info("Account unlocked: username={}, admin={}", user.getUsername(), adminUsername);
    }

    @Override
    public boolean isCurrentlyLocked(User user) {
        if (user == null || user.getLockedUntil() == null) {
            return false;
        }
        return user.getLockedUntil().isAfter(LocalDateTime.now());
    }

    @Override
    @Transactional
    public void clearExpiredLock(User user) {
        if (user.getLockedUntil() != null && !user.getLockedUntil().isAfter(LocalDateTime.now())) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public int purgeExpiredRateLimitRows() {
        return deleteExpiredRateLimitRows();
    }

    @Transactional
    public int deleteExpiredRateLimitRows() {
        LocalDateTime cutoff = LocalDateTime.now()
                .minusSeconds(properties.rateLimit().windowSeconds());
        return rateLimitRepository.deleteExpiredWindows(cutoff);
    }

    private boolean isBucketExceeded(String prefix, String normalizedValue) {
        String rateKey = LoginRateLimitKeys.storageKey(prefix, normalizedValue);
        Optional<LoginRateLimit> bucket = rateLimitRepository.findByRateKey(rateKey);
        if (bucket.isEmpty()) {
            return false;
        }
        LoginRateLimit entry = bucket.get();
        if (windowExpired(entry)) {
            return false;
        }
        return entry.getAttemptCount() >= properties.rateLimit().maxAttempts();
    }

    private void incrementBucket(String prefix, String normalizedValue) {
        String rateKey = LoginRateLimitKeys.storageKey(prefix, normalizedValue);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowNotBefore = now.minusSeconds(properties.rateLimit().windowSeconds());
        loginRateLimitUpsert.upsertIncrement(UUID.randomUUID(), rateKey, now, windowNotBefore);
    }

    private boolean windowExpired(LoginRateLimit entry) {
        return entry.getWindowStart()
                .plusSeconds(properties.rateLimit().windowSeconds())
                .isBefore(LocalDateTime.now());
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "_empty_";
        }
        return value.trim().toLowerCase();
    }
}
