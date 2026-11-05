package app.model.filter;

import app.model.enums.InvoiceStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class InvoiceListFilter {

    private UUID customerId;
    private InvoiceStatus status;
    private Integer periodYear;
    private Integer periodMonth;
    private boolean showCancelled;
}
