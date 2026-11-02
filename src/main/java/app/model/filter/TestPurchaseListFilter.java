package app.model.filter;

import app.model.enums.TestPurchaseStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class TestPurchaseListFilter {

    private TestPurchaseStatus status;

    private UUID customerId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdTo;

    private UUID mysteryShopperId;
}
