package app.service.impl;

import app.audit.AuditChangeSummary;
import app.client.PricingClient;
import app.config.SecurityUtils;
import app.model.dto.MonthlyStatementDTO;
import app.model.dto.MonthlyStatementPurchaseRowDTO;
import app.model.dto.PriceCalculationRequestDTO;
import app.model.dto.PriceCalculationResponseDTO;
import app.model.entity.Customer;
import app.model.entity.Invoice;
import app.model.entity.InvoiceLineItem;
import app.model.entity.Item;
import app.model.entity.TestPurchase;
import app.model.enums.AuditAction;
import app.model.enums.AuditEntityType;
import app.model.enums.InvoiceLineType;
import app.model.enums.InvoiceStatus;
import app.model.filter.InvoiceListFilter;
import app.repository.CustomerRepository;
import app.repository.InvoiceLineItemRepository;
import app.repository.InvoiceRepository;
import app.repository.TestPurchaseRepository;
import app.repository.spec.InvoiceSpecifications;
import app.service.AccessControlService;
import app.service.AuditLogService;
import app.service.EntityNumberAllocator;
import app.service.InvoiceService;
import app.support.InvoicePdfWriter;
import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private static final String DEFAULT_CURRENCY = "EUR";

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final CustomerRepository customerRepository;
    private final TestPurchaseRepository testPurchaseRepository;
    private final AccessControlService accessControlService;
    private final EntityNumberAllocator entityNumberAllocator;
    private final PricingClient pricingClient;
    private final AuditLogService auditLogService;
    private final InvoicePdfWriter invoicePdfWriter;
    private final MessageSource messageSource;

    @Override
    @Transactional(readOnly = true)
    public Page<Invoice> findPageForCurrentUser(InvoiceListFilter filter, Pageable pageable) {
        var user = accessControlService.getCurrentUserWithManagedCustomers();
        var spec = InvoiceSpecifications.listQuery(user, filter);
        return invoiceRepository.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice findByIdForCurrentUser(UUID id) {
        Invoice invoice = invoiceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        accessControlService.checkCanViewInvoice(invoice);
        return invoice;
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyStatementDTO buildMonthlyStatement(UUID customerId, int year, int month) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        accessControlService.checkCanViewCustomer(customer);
        validatePeriod(year, month);

        YearMonth ym = YearMonth.of(year, month);
        LocalDateTime periodStart = ym.atDay(1).atStartOfDay();
        LocalDateTime periodEnd = ym.plusMonths(1).atDay(1).atStartOfDay();

        Set<UUID> invoicedPurchaseIds = invoiceLineItemRepository.findTestPurchaseIdsOnNonCancelledInvoices();
        List<TestPurchase> purchases = testPurchaseRepository.findClosedForCustomerInPeriod(
                customerId, periodStart, periodEnd);

        List<MonthlyStatementPurchaseRowDTO> rows = new ArrayList<>();
        double purchasesTotal = 0.0;
        for (TestPurchase purchase : purchases) {
            PriceCalculationResponseDTO pricing = priceForPurchase(purchase);
            double lineTotal = pricing.getTestPurchaseFee() + pricing.getPostageFee() + pricing.getProductPrice();
            boolean alreadyInvoiced = invoicedPurchaseIds.contains(purchase.getId());
            rows.add(MonthlyStatementPurchaseRowDTO.builder()
                    .testPurchaseId(purchase.getId())
                    .testPurchaseNumber(purchase.getNumber())
                    .serviceFee(pricing.getTestPurchaseFee())
                    .postageFee(pricing.getPostageFee())
                    .productPrice(pricing.getProductPrice())
                    .lineTotal(lineTotal)
                    .alreadyInvoiced(alreadyInvoiced)
                    .build());
            if (!alreadyInvoiced) {
                purchasesTotal += lineTotal;
            }
        }

        double baseFee = baseFeeForCustomer(customer);
        double grandTotal = purchasesTotal + baseFee;
        boolean hasExisting = invoiceRepository.existsActiveInvoiceForPeriod(
                customerId, year, month, InvoiceStatus.CANCELLED);

        return MonthlyStatementDTO.builder()
                .customerId(customer.getId())
                .customerName(customer.getName())
                .periodYear(year)
                .periodMonth(month)
                .currency(DEFAULT_CURRENCY)
                .baseFeeAmount(baseFee)
                .purchasesTotal(purchasesTotal)
                .grandTotal(grandTotal)
                .purchases(rows)
                .hasExistingInvoice(hasExisting)
                .build();
    }

    @Override
    @Transactional
    public Invoice createDraftFromStatement(UUID customerId, int year, int month, String notes) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        accessControlService.checkCanManageInvoicesForCustomer(customer);
        validatePeriod(year, month);

        if (invoiceRepository.existsActiveInvoiceForPeriod(customerId, year, month, InvoiceStatus.CANCELLED)) {
            throw new IllegalStateException("An invoice already exists for this customer and period");
        }

        MonthlyStatementDTO statement = buildMonthlyStatement(customerId, year, month);
        if (statement.getPurchases().stream().noneMatch(p -> !p.isAlreadyInvoiced())
                && statement.getBaseFeeAmount() <= 0.0) {
            throw new IllegalStateException("No billable items for this period");
        }

        Invoice invoice = new Invoice();
        invoice.setNumber(entityNumberAllocator.nextInvoiceNumber());
        invoice.setCustomer(customer);
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setPeriodYear(year);
        invoice.setPeriodMonth(month);
        invoice.setCurrency(DEFAULT_CURRENCY);
        invoice.setNotes(notes);
        invoice.setUpdatedByUser(SecurityUtils.getCurrentUsername());

        List<InvoiceLineItem> lines = new ArrayList<>();
        int sort = 0;
        for (MonthlyStatementPurchaseRowDTO row : statement.getPurchases()) {
            if (row.isAlreadyInvoiced()) {
                continue;
            }
            TestPurchase purchase = testPurchaseRepository.findById(row.getTestPurchaseId())
                    .orElseThrow();
            lines.add(InvoiceLineItem.builder()
                    .invoice(invoice)
                    .testPurchase(purchase)
                    .lineType(InvoiceLineType.TEST_PURCHASE)
                    .description(purchase.getNumber())
                    .quantity(1)
                    .unitAmount(row.getLineTotal())
                    .lineTotal(row.getLineTotal())
                    .sortOrder(sort++)
                    .build());
        }
        if (statement.getBaseFeeAmount() > 0) {
            lines.add(InvoiceLineItem.builder()
                    .invoice(invoice)
                    .lineType(InvoiceLineType.BASE_FEE)
                    .description(messageSource.getMessage(
                            "invoice.line.baseFee", null, "Base service fee", Locale.getDefault()))
                    .quantity(1)
                    .unitAmount(statement.getBaseFeeAmount())
                    .lineTotal(statement.getBaseFeeAmount())
                    .sortOrder(sort++)
                    .build());
        }
        invoice.setLineItems(lines);
        invoice.setTotalAmount(lines.stream().mapToDouble(InvoiceLineItem::getLineTotal).sum());

        Invoice saved = invoiceRepository.save(invoice);
        auditLogService.record(
                AuditAction.INVOICE_CREATE,
                AuditEntityType.INVOICE,
                saved.getId(),
                AuditChangeSummary.join(
                        AuditChangeSummary.kv("number", saved.getNumber()),
                        AuditChangeSummary.kv("status", saved.getStatus()),
                        AuditChangeSummary.kv("customer", customer.getName()),
                        AuditChangeSummary.kv("period", year + "-" + month)));
        return saved;
    }

    @Override
    @Transactional
    public Invoice issue(UUID id) {
        Invoice invoice = loadForManage(id);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only draft invoices can be issued");
        }
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setUpdatedByUser(SecurityUtils.getCurrentUsername());
        Invoice saved = invoiceRepository.save(invoice);
        auditLogService.record(
                AuditAction.INVOICE_ISSUE,
                AuditEntityType.INVOICE,
                saved.getId(),
                AuditChangeSummary.kv("number", saved.getNumber()));
        return saved;
    }

    @Override
    @Transactional
    public Invoice markPaid(UUID id) {
        Invoice invoice = loadForManage(id);
        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new IllegalStateException("Only issued invoices can be marked paid");
        }
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        invoice.setUpdatedByUser(SecurityUtils.getCurrentUsername());
        Invoice saved = invoiceRepository.save(invoice);
        auditLogService.record(
                AuditAction.INVOICE_MARK_PAID,
                AuditEntityType.INVOICE,
                saved.getId(),
                AuditChangeSummary.kv("number", saved.getNumber()));
        return saved;
    }

    @Override
    @Transactional
    public Invoice cancel(UUID id) {
        Invoice invoice = loadForManage(id);
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Invoice is already cancelled");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Paid invoices cannot be cancelled");
        }
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setCancelledAt(LocalDateTime.now());
        invoice.setUpdatedByUser(SecurityUtils.getCurrentUsername());
        Invoice saved = invoiceRepository.save(invoice);
        auditLogService.record(
                AuditAction.INVOICE_CANCEL,
                AuditEntityType.INVOICE,
                saved.getId(),
                AuditChangeSummary.kv("number", saved.getNumber()));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] buildPdf(UUID id, Locale locale) throws DocumentException {
        Invoice invoice = findByIdForCurrentUser(id);
        return invoicePdfWriter.write(invoice, locale, messageSource);
    }

    private Invoice loadForManage(UUID id) {
        Invoice invoice = invoiceRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        accessControlService.checkCanManageInvoicesForCustomer(invoice.getCustomer());
        return invoice;
    }

    private void validatePeriod(int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid month");
        }
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Invalid year");
        }
    }

    private double baseFeeForCustomer(Customer customer) {
        if (customer.getBaseServiceFee() == null || customer.getBaseServiceFee() <= 0) {
            return 0.0;
        }
        return customer.getBaseServiceFee();
    }

    private PriceCalculationResponseDTO priceForPurchase(TestPurchase purchase) {
        double productTotal = itemsTotalPrice(purchase);
        PriceCalculationRequestDTO request = new PriceCalculationRequestDTO();
        request.setCustomerId(purchase.getCustomer().getId());
        request.setCountry(purchase.getCountry().name());
        request.setCategory(purchase.getCategory().name());
        request.setType(purchase.getType().name());
        request.setProductTotal(productTotal);
        return pricingClient.calculatePrice(request);
    }

    private static double itemsTotalPrice(TestPurchase purchase) {
        if (purchase.getItems() == null || purchase.getItems().isEmpty()) {
            return purchase.getProductPrice() != null ? purchase.getProductPrice() : 0.0;
        }
        double total = 0.0;
        for (Item item : purchase.getItems()) {
            total += item.getQuantity() * item.getUnitPrice();
        }
        return total;
    }
}
