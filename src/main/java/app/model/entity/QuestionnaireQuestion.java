package app.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "questionnaire_questions")
public class QuestionnaireQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private QuestionnaireTemplate template;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "message_key", nullable = false, length = 120)
    private String messageKey;

    @Column(name = "max_score", nullable = false)
    private int maxScore;

    @Column(nullable = false)
    private int weight;

    @Column(nullable = false)
    private boolean active;
}
