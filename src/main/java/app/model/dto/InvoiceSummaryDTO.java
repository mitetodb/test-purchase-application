package app.model.dto;

import app.model.enums.InvoiceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class InvoiceSummaryDTO {

    private UUID id;
    private String number;
    private UUID customerId;
    private String customerName;
    private InvoiceStatus status;
    private int periodYear;
    private int periodMonth;
    private String currency;
    private double totalAmount;
    private LocalDateTime issuedAt;
}
