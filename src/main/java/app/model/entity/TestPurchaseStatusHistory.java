package app.model.entity;

import app.model.enums.TestPurchaseStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "testpurchase_status_history")
@Entity
public class TestPurchaseStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "testpurchase_id", nullable = false)
    private TestPurchase testPurchase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestPurchaseStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestPurchaseStatus newStatus;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    @Column(nullable = false)
    private String changedBy;

    @Column
    private String comment;

}

