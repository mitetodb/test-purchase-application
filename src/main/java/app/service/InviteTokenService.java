package app.service;

import app.model.entity.InviteToken;
import app.model.entity.User;
import app.model.enums.Role;
import app.repository.InviteTokenRepository;
import app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class InviteTokenService {

    private final InviteTokenRepository inviteTokenRepository;
    private final UserRepository userRepository;
    private final AccessControlService accessControlService;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Create a new invite token. Only ADMIN and ACCOUNT_MANAGER can create tokens.
     * @return the raw token string (only shown once)
     */
    @Transactional
    public String createInviteToken() {
        User currentUser = accessControlService.getCurrentUser();
        
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.ACCOUNT_MANAGER) {
            throw new AccessDeniedException("Only ADMIN and ACCOUNT_MANAGER can create invite tokens");
        }

        // Generate a random token (32 bytes = 256 bits)
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        // Hash the token for storage
        String tokenHash = hashToken(rawToken);

        InviteToken inviteToken = InviteToken.builder()
                .tokenHash(tokenHash)
                .createdBy(currentUser)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        inviteTokenRepository.save(inviteToken);

        return rawToken;
    }

    /**
     * Validate and consume an invite token during registration.
     * @param rawToken the raw token from the URL
     * @param registeredUser the user who just registered
     * @throws IllegalArgumentException if token is invalid, used, or expired
     */
    @Transactional
    public void validateAndConsumeToken(String rawToken, User registeredUser) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException("Invite token is required");
        }

        String tokenHash = hashToken(rawToken);
        InviteToken inviteToken = inviteTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite token"));

        if (inviteToken.isUsed()) {
            throw new IllegalArgumentException("Invite token has already been used");
        }

        if (inviteToken.isExpired()) {
            throw new IllegalArgumentException("Invite token has expired");
        }

        // Mark token as used
        inviteToken.setUsedAt(LocalDateTime.now());
        inviteToken.setUsedBy(registeredUser);
        inviteTokenRepository.save(inviteToken);
    }

    /**
     * Hash a token using SHA-256.
     */
    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
