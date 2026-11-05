package app.model.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class MonthlyStatementPurchaseRowDTO {

    private UUID testPurchaseId;
    private String testPurchaseNumber;
    private double serviceFee;
    private double postageFee;
    private double productPrice;
    private double lineTotal;
    private boolean alreadyInvoiced;
}
