package app.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InvoiceLineType {
    TEST_PURCHASE("Test purchase"),
    BASE_FEE("Base fee"),
    ADJUSTMENT("Adjustment");

    private final String displayName;
}
