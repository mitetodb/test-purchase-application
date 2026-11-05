package app.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "test_purchase_deadline_reminders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_tp_deadline_reminder",
                columnNames = {"test_purchase_id", "days_before_offset"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestPurchaseDeadlineReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "test_purchase_id", nullable = false)
    private TestPurchase testPurchase;

    @Column(name = "days_before_offset", nullable = false)
    private int daysBeforeOffset;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
}
