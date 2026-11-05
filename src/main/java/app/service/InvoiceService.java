package app.service;

import app.model.dto.MonthlyStatementDTO;
import app.model.entity.Invoice;
import app.model.filter.InvoiceListFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface InvoiceService {

    Page<Invoice> findPageForCurrentUser(InvoiceListFilter filter, Pageable pageable);

    Invoice findByIdForCurrentUser(UUID id);

    MonthlyStatementDTO buildMonthlyStatement(UUID customerId, int year, int month);

    Invoice createDraftFromStatement(UUID customerId, int year, int month, String notes);

    Invoice issue(UUID id);

    Invoice markPaid(UUID id);

    Invoice cancel(UUID id);

    byte[] buildPdf(UUID id, java.util.Locale locale) throws com.lowagie.text.DocumentException;
}
