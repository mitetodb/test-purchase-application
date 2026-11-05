package app.support;

import app.model.entity.Invoice;
import app.model.entity.InvoiceLineItem;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class InvoicePdfWriter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] write(Invoice invoice, Locale locale, MessageSource messageSource) throws DocumentException {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        document.add(new Paragraph(msg(messageSource, "invoice.pdf.title", locale), titleFont));
        document.add(blank(bodyFont));

        addField(document, labelFont, bodyFont, messageSource, "ui.number.2a39", locale, invoice.getNumber());
        addField(document, labelFont, bodyFont, messageSource, "ui.customer.5eea", locale,
                invoice.getCustomer() != null ? invoice.getCustomer().getName() : "");
        addField(document, labelFont, bodyFont, messageSource, "invoice.field.period", locale,
                invoice.getPeriodYear() + "-" + String.format("%02d", invoice.getPeriodMonth()));
        addField(document, labelFont, bodyFont, messageSource, "ui.status.24a2", locale,
                invoice.getStatus() != null ? invoice.getStatus().getDisplayName() : "");
        if (invoice.getIssuedAt() != null) {
            addField(document, labelFont, bodyFont, messageSource, "invoice.field.issuedAt", locale,
                    invoice.getIssuedAt().format(DATE_TIME));
        }

        document.add(blank(bodyFont));
        document.add(new Paragraph(msg(messageSource, "invoice.pdf.lines", locale), labelFont));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.addCell(headerCell(msg(messageSource, "invoice.column.description", locale), labelFont));
        table.addCell(headerCell(msg(messageSource, "ui.qty.03ab", locale), labelFont));
        table.addCell(headerCell(msg(messageSource, "invoice.column.unitAmount", locale), labelFont));
        table.addCell(headerCell(msg(messageSource, "invoice.column.lineTotal", locale), labelFont));

        if (invoice.getLineItems() != null) {
            for (InvoiceLineItem line : invoice.getLineItems()) {
                table.addCell(bodyCell(line.getDescription(), bodyFont));
                table.addCell(bodyCell(String.valueOf(line.getQuantity()), bodyFont));
                table.addCell(bodyCell(formatMoney(line.getUnitAmount(), invoice.getCurrency()), bodyFont));
                table.addCell(bodyCell(formatMoney(line.getLineTotal(), invoice.getCurrency()), bodyFont));
            }
        }
        document.add(table);

        document.add(blank(bodyFont));
        addField(document, labelFont, bodyFont, messageSource, "invoice.field.total", locale,
                formatMoney(invoice.getTotalAmount(), invoice.getCurrency()));

        document.add(blank(bodyFont));
        document.add(new Paragraph(msg(messageSource, "invoice.pdf.bookkeepingNotice", locale), bodyFont));

        document.close();
        return out.toByteArray();
    }

    private static String formatMoney(double amount, String currency) {
        return String.format(Locale.ROOT, "%.2f %s", amount, currency != null ? currency : "EUR");
    }

    private static PdfPCell headerCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }

    private static PdfPCell bodyCell(String text, Font font) {
        return new PdfPCell(new Phrase(text, font));
    }

    private static Paragraph blank(Font font) {
        return new Paragraph(" ", font);
    }

    private static void addField(
            Document document,
            Font labelFont,
            Font bodyFont,
            MessageSource messageSource,
            String labelKey,
            Locale locale,
            String value) throws DocumentException {
        document.add(new Paragraph(
                msg(messageSource, labelKey, locale) + ": " + value, bodyFont));
    }

    private static String msg(MessageSource messageSource, String code, Locale locale) {
        return messageSource.getMessage(code, null, code, locale);
    }
}
