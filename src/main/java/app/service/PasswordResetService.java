package app.service;

import app.model.entity.PasswordResetToken;
import app.model.entity.User;
import app.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;

    public String createTokenForUser(User user) {
        String token = UUID.randomUUID().toString();

        PasswordResetToken prt = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusHours(2))
                .used(false)
                .build();

        tokenRepository.save(prt);
        return token;
    }

    public PasswordResetToken validateToken(String token) {
        PasswordResetToken prt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));

        if (prt.isUsed()) {
            throw new IllegalArgumentException("Token already used");
        }
        if (prt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expired");
        }

        return prt;
    }

    public void markUsed(PasswordResetToken token) {
        token.setUsed(true);
        tokenRepository.save(token);
    }

}
