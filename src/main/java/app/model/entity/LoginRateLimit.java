package app.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "login_rate_limit")
public class LoginRateLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rate_key", nullable = false, unique = true, length = 64)
    private String rateKey;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;
}
