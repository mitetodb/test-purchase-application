package app.model.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class ScoreTrendMonthDTO {
    int year;
    int month;
    BigDecimal averageScore;
    long responseCount;
}
