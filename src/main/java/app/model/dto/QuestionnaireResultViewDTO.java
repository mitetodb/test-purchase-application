package app.model.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class QuestionnaireResultViewDTO {
    UUID testPurchaseId;
    String testPurchaseNumber;
    BigDecimal overallScore;
    LocalDateTime submittedAt;
    boolean hideEvaluatorIdentity;
    String evaluatorDisplayName;
    List<QuestionnaireQuestionViewDTO> questions;
}
