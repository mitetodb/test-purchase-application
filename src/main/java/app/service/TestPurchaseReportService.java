package app.service;

import app.model.dto.TestPurchaseViewDTO;
import app.support.TestPurchasePdfReportWriter;
import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TestPurchaseReportService {

    private final TestPurchaseService testPurchaseService;
    private final AttachmentService attachmentService;
    private final StatusHistoryService statusHistoryService;
    private final TestPurchasePdfReportWriter pdfReportWriter;
    private final MessageSource messageSource;

    public record PdfReport(byte[] content, String downloadFilename) {
    }

    @Transactional(readOnly = true)
    public PdfReport buildPdfReport(UUID testPurchaseId, Locale locale) throws DocumentException {
        TestPurchaseViewDTO purchase = testPurchaseService.findByIdForCurrentUser(testPurchaseId);
        var attachments = attachmentService.getByTestPurchase(testPurchaseId);
        var history = statusHistoryService.getHistory(testPurchaseId);
        byte[] content = pdfReportWriter.write(purchase, attachments, history, locale, messageSource);
        String filename = "test-purchase-" + sanitizeFilename(purchase.getNumber()) + ".pdf";
        return new PdfReport(content, filename);
    }

    private static String sanitizeFilename(String number) {
        if (number == null || number.isBlank()) {
            return "report";
        }
        return number.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
