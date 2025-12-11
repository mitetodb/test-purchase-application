package app.service;

import app.model.entity.TestPurchase;
import app.model.enums.TestPurchaseStatus;
import app.repository.PasswordResetTokenRepository;
import app.repository.TestPurchaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledTasksService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final TestPurchaseRepository testPurchaseRepository;

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredPasswordResetTokens() {
        log.info("Starting scheduled task: cleanup expired password reset tokens");
        LocalDateTime now = LocalDateTime.now();
        List<app.model.entity.PasswordResetToken> expiredTokens = 
                passwordResetTokenRepository.findExpiredOrUsedTokens(now);
        
        int count = expiredTokens.size();
        if (count > 0) {
            passwordResetTokenRepository.deleteExpiredOrUsedTokens(now);
            log.info("Cleaned up {} expired or used password reset tokens", count);
        } else {
            log.info("No expired or used password reset tokens to clean up");
        }
    }

    @Scheduled(fixedDelay = 21600000)
    @Transactional
    public void updateTestPurchaseStatistics() {
        log.info("Starting scheduled task: update test purchase statistics");
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        
        List<TestPurchase> oldCompletedPurchases = testPurchaseRepository.findAll()
                .stream()
                .filter(tp -> tp.getStatus() == TestPurchaseStatus.CLOSED 
                        && tp.getUpdatedOn() != null 
                        && tp.getUpdatedOn().isBefore(oneMonthAgo))
                .toList();
        
        log.info("Found {} closed test purchases older than 1 month", oldCompletedPurchases.size());
        
        if (!oldCompletedPurchases.isEmpty()) {
            log.info("These purchases can be archived or reviewed for reporting purposes");
        }
    }
}

