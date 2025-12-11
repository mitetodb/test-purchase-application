package app.service;

import app.model.entity.PasswordResetToken;
import app.model.entity.TestPurchase;
import app.model.enums.TestPurchaseStatus;
import app.repository.PasswordResetTokenRepository;
import app.repository.TestPurchaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledTasksServiceTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private TestPurchaseRepository testPurchaseRepository;

    @InjectMocks
    private ScheduledTasksService scheduledTasksService;

    @Test
    void testCleanupExpiredPasswordResetTokens() {
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .token("expired-token")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .used(false)
                .build();

        when(passwordResetTokenRepository.findExpiredOrUsedTokens(any(LocalDateTime.class)))
                .thenReturn(List.of(expiredToken));

        scheduledTasksService.cleanupExpiredPasswordResetTokens();

        verify(passwordResetTokenRepository).findExpiredOrUsedTokens(any(LocalDateTime.class));
        verify(passwordResetTokenRepository).deleteExpiredOrUsedTokens(any(LocalDateTime.class));
    }

    @Test
    void testCleanupExpiredPasswordResetTokensNoTokens() {
        when(passwordResetTokenRepository.findExpiredOrUsedTokens(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());

        scheduledTasksService.cleanupExpiredPasswordResetTokens();

        verify(passwordResetTokenRepository).findExpiredOrUsedTokens(any(LocalDateTime.class));
        verify(passwordResetTokenRepository, never()).deleteExpiredOrUsedTokens(any(LocalDateTime.class));
    }

    @Test
    void testUpdateTestPurchaseStatistics() {
        TestPurchase completedPurchase = TestPurchase.builder()
                .id(UUID.randomUUID())
                .status(TestPurchaseStatus.CLOSED)
                .updatedOn(LocalDateTime.now().minusMonths(2))
                .build();

        when(testPurchaseRepository.findAll()).thenReturn(List.of(completedPurchase));

        scheduledTasksService.updateTestPurchaseStatistics();

        verify(testPurchaseRepository).findAll();
    }
}

