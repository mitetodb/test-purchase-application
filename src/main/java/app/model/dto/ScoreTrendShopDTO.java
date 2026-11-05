package app.model.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class ScoreTrendShopDTO {
    UUID shopId;
    String shopName;
    BigDecimal averageScore;
    long responseCount;
}
