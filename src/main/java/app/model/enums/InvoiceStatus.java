package app.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InvoiceStatus {
    DRAFT("Draft"),
    ISSUED("Issued"),
    PAID("Paid"),
    CANCELLED("Cancelled");

    private final String displayName;
}
