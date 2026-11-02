package app.service;

import java.util.UUID;

public interface LoginSecurityService {

    boolean isRateLimitExceeded(String clientIp, String username);

    void recordLoginAttempt(String clientIp, String username);

    void handleFailedLogin(String clientIp, String username);

    void handleSuccessfulLogin(String username);

    void unlockAccount(UUID userId, String adminUsername);

    boolean isCurrentlyLocked(app.model.entity.User user);

    void clearExpiredLock(app.model.entity.User user);

    int purgeExpiredRateLimitRows();
}
