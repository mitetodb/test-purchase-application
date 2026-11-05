package app.model.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class QuestionnaireQuestionViewDTO {
    UUID id;
    int sortOrder;
    String prompt;
    int maxScore;
    int weight;
    Integer givenScore;
}
