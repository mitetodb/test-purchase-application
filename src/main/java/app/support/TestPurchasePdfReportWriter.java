package app.support;

import app.model.dto.TestPurchaseViewDTO;
import app.model.entity.Attachment;
import app.model.entity.Item;
import app.model.entity.TestPurchaseStatusHistory;
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
import java.util.List;
import java.util.Locale;

@Component
public class TestPurchasePdfReportWriter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public byte[] write(
            TestPurchaseViewDTO purchase,
            List<Attachment> attachments,
            List<TestPurchaseStatusHistory> history,
            Locale locale,
            MessageSource messageSource) throws DocumentException {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        document.add(new Paragraph(msg(messageSource, "report.testPurchase.title", locale), titleFont));
        document.add(blank(bodyFont));

        addField(document, labelFont, bodyFont, messageSource, "ui.number.2a39", locale,
                purchase.getNumber());
        addField(document, labelFont, bodyFont, messageSource, "ui.customer.5eea", locale,
                purchase.getCustomer() != null ? purchase.getCustomer().getName() : "");
        addField(document, labelFont, bodyFont, messageSource, "ui.shop.b91c", locale,
                purchase.getShop() != null ? purchase.getShop().getName() : "");
        addField(document, labelFont, bodyFont, messageSource, "ui.country.f64b", locale,
                purchase.getCountry() != null ? purchase.getCountry().getDisplayName() : "");
        addField(document, labelFont, bodyFont, messageSource, "ui.category.56a8", locale,
                purchase.getCategory() != null ? purchase.getCategory().getDisplayName() : "");
        addField(document, labelFont, bodyFont, messageSource, "ui.type.e659", locale,
                purchase.getType() != null ? purchase.getType().getDisplayName() : "");
        addField(document, labelFont, bodyFont, messageSource, "ui.status.24a2", locale,
                purchase.getStatus() != null ? purchase.getStatus().getDisplayName() : "");
        addField(document, labelFont, bodyFont, messageSource, "ui.total_price.614d", locale,
                String.valueOf(purchase.getProductPrice()));
        addField(document, labelFont, bodyFont, messageSource, "ui.service_fee.eb23", locale,
                String.valueOf(purchase.getServiceFee()));
        addField(document, labelFont, bodyFont, messageSource, "ui.postage_fee.518c", locale,
                String.valueOf(purchase.getPostageFee()));
        addField(document, labelFont, bodyFont, messageSource, "report.label.createdOn", locale,
                purchase.getCreatedDate() != null ? purchase.getCreatedDate().format(DATE_TIME) : "");

        String mysteryShopperValue;
        if (purchase.isMysteryShopperHidden()) {
            mysteryShopperValue = msg(messageSource, "report.mysteryShopper.hidden", locale);
        } else if (purchase.getMysteryShopper() != null) {
            mysteryShopperValue = purchase.getMysteryShopper().getUsername();
        } else {
            mysteryShopperValue = msg(messageSource, "report.mysteryShopper.unassigned", locale);
        }
        addField(document, labelFont, bodyFont, messageSource, "report.label.mysteryShopper", locale,
                mysteryShopperValue);

        document.add(blank(bodyFont));
        document.add(new Paragraph(msg(messageSource, "ui.items.9dea", locale), labelFont));

        PdfPTable itemsTable = new PdfPTable(5);
        itemsTable.setWidthPercentage(100);
        itemsTable.addCell(headerCell(msg(messageSource, "ui.url.e6b3", locale), labelFont));
        itemsTable.addCell(headerCell(msg(messageSource, "ui.product.deb1", locale), labelFont));
        itemsTable.addCell(headerCell(msg(messageSource, "ui.qty.03ab", locale), labelFont));
        itemsTable.addCell(headerCell(msg(messageSource, "ui.unit_price.1971", locale), labelFont));
        itemsTable.addCell(headerCell(msg(messageSource, "ui.subtotal.2194", locale), labelFont));

        if (purchase.getItems() != null) {
            for (Item item : purchase.getItems()) {
                itemsTable.addCell(bodyCell(nullToEmpty(item.getProductUrl()), bodyFont));
                itemsTable.addCell(bodyCell(nullToEmpty(item.getProductName()), bodyFont));
                itemsTable.addCell(bodyCell(String.valueOf(item.getQuantity()), bodyFont));
                itemsTable.addCell(bodyCell(String.valueOf(item.getUnitPrice()), bodyFont));
                double subtotal = item.getQuantity() * item.getUnitPrice();
                itemsTable.addCell(bodyCell(String.valueOf(subtotal), bodyFont));
            }
        }
        document.add(itemsTable);

        document.add(blank(bodyFont));
        document.add(new Paragraph(msg(messageSource, "ui.attachments.7e27", locale), labelFont));
        document.add(new Paragraph(msg(messageSource, "report.attachments.note", locale), bodyFont));
        if (attachments == null || attachments.isEmpty()) {
            document.add(new Paragraph(msg(messageSource, "report.attachments.none", locale), bodyFont));
        } else {
            for (Attachment att : attachments) {
                String line = att.getName();
                if (att.getCategory() != null) {
                    line += " (" + att.getCategory().getDisplayName() + ")";
                }
                document.add(new Paragraph("• " + line, bodyFont));
            }
        }

        document.add(blank(bodyFont));
        document.add(new Paragraph(msg(messageSource, "ui.status_history.b421", locale), labelFont));
        if (history == null || history.isEmpty()) {
            document.add(new Paragraph(msg(messageSource, "report.statusHistory.none", locale), bodyFont));
        } else {
            for (TestPurchaseStatusHistory h : history) {
                String when = h.getChangedAt() != null ? h.getChangedAt().format(DATE_TIME) : "";
                String transition = h.getOldStatus().getDisplayName() + " → " + h.getNewStatus().getDisplayName();
                String by = msg(messageSource, "ui.by.8f55", locale) + " " + h.getChangedBy();
                document.add(new Paragraph(when + " — " + transition, bodyFont));
                document.add(new Paragraph(by, bodyFont));
                if (h.getComment() != null && !h.getComment().isBlank()) {
                    document.add(new Paragraph(h.getComment(), bodyFont));
                }
                document.add(blank(bodyFont));
            }
        }

        document.close();
        return out.toByteArray();
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
        document.add(new Paragraph(msg(messageSource, labelKey, locale) + " " + value, bodyFont));
    }

    private static String msg(MessageSource messageSource, String code, Locale locale) {
        return messageSource.getMessage(code, null, code, locale);
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
