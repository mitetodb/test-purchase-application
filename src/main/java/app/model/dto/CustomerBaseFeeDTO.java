package app.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CustomerBaseFeeDTO {

    private UUID customerId;
    private Double baseServiceFee;
    private LocalDateTime changedAt;
}