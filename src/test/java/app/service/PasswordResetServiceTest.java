package app.service;

import app.model.entity.PasswordResetToken;
import app.model.entity.User;
import app.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User user;
    private PasswordResetToken token;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .build();

        token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .token("test-token")
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(2))
                .used(false)
                .build();
    }

    @Test
    void testCreateTokenForUser() {
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String result = passwordResetService.createTokenForUser(user);

        assertThat(result).isNotBlank();
        verify(tokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void testValidateToken() {
        when(tokenRepository.findByToken("test-token")).thenReturn(Optional.of(token));

        PasswordResetToken result = passwordResetService.validateToken("test-token");

        assertThat(result).isEqualTo(token);
        assertThat(result.isUsed()).isFalse();
    }

    @Test
    void testValidateTokenNotFound() {
        when(tokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.validateToken("invalid-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid token");
    }

    @Test
    void testValidateTokenAlreadyUsed() {
        token.setUsed(true);
        when(tokenRepository.findByToken("test-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.validateToken("test-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token already used");
    }

    @Test
    void testValidateTokenExpired() {
        token.setExpiresAt(LocalDateTime.now().minusHours(1));
        when(tokenRepository.findByToken("test-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.validateToken("test-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Token expired");
    }

    @Test
    void testMarkUsed() {
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        passwordResetService.markUsed(token);

        assertThat(token.isUsed()).isTrue();
        verify(tokenRepository).save(token);
    }
}

