package app.model.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class MonthlyStatementDTO {

    private UUID customerId;
    private String customerName;
    private int periodYear;
    private int periodMonth;
    private String currency;
    private double baseFeeAmount;
    private double purchasesTotal;
    private double grandTotal;
    private List<MonthlyStatementPurchaseRowDTO> purchases;
    private boolean hasExistingInvoice;
}
