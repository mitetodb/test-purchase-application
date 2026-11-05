package app.controller;

import app.model.entity.Invoice;
import app.model.enums.Role;
import app.model.filter.InvoiceListFilter;
import app.service.AccessControlService;
import app.service.CustomerService;
import app.service.InvoiceService;
import app.support.ListPagingSupport;
import com.lowagie.text.DocumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.YearMonth;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final CustomerService customerService;
    private final ListPagingSupport listPagingSupport;
    private final AccessControlService accessControlService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER','SALES_MANAGER','CUSTOMER')")
    public String list(
            @ModelAttribute("filter") InvoiceListFilter filter,
            @PageableDefault(size = 20, sort = "createdOn") Pageable pageable,
            Model model) {
        Pageable safe = listPagingSupport.forInvoices(pageable);
        Page<Invoice> page = invoiceService.findPageForCurrentUser(filter, safe);
        model.addAttribute("invoicePage", page);
        model.addAttribute("invoices", page.getContent());
        model.addAttribute("customers", customerService.findAllForCurrentUser());
        model.addAttribute("invoiceStatuses", app.model.enums.InvoiceStatus.values());
        model.addAttribute("showCancelled", filter.isShowCancelled());
        return "invoices/invoices-list";
    }

    @GetMapping("/statement")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER','SALES_MANAGER','CUSTOMER')")
    public String statement(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Model model) {
        var currentUser = accessControlService.getCurrentUserWithManagedCustomers();
        if (currentUser.getRole() == Role.CUSTOMER && currentUser.getCustomer() != null) {
            customerId = currentUser.getCustomer().getId();
        }
        model.addAttribute("customers", customerService.findAllForCurrentUser());
        model.addAttribute("selectedCustomerId", customerId);
        YearMonth ym = resolvePeriod(year, month);
        model.addAttribute("selectedYear", ym.getYear());
        model.addAttribute("selectedMonth", ym.getMonthValue());
        if (customerId == null) {
            return "invoices/invoice-statement";
        }
        var statement = invoiceService.buildMonthlyStatement(customerId, ym.getYear(), ym.getMonthValue());
        model.addAttribute("statement", statement);
        return "invoices/invoice-statement";
    }

    @PostMapping("/create-from-statement")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER')")
    public String createFromStatement(
            @RequestParam UUID customerId,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {
        try {
            Invoice invoice = invoiceService.createDraftFromStatement(customerId, year, month, notes);
            redirectAttributes.addFlashAttribute("successMessage", "invoice.flash.created");
            return "redirect:/invoices/" + invoice.getId();
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/invoices/statement?customerId=" + customerId + "&year=" + year + "&month=" + month;
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER','SALES_MANAGER','CUSTOMER')")
    public String view(@PathVariable UUID id, Model model) {
        Invoice invoice = invoiceService.findByIdForCurrentUser(id);
        model.addAttribute("invoice", invoice);
        model.addAttribute("canManage", canManage(invoice));
        return "invoices/invoice-view";
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER','SALES_MANAGER','CUSTOMER')")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) throws DocumentException {
        Invoice invoice = invoiceService.findByIdForCurrentUser(id);
        byte[] pdf = invoiceService.buildPdf(id, org.springframework.context.i18n.LocaleContextHolder.getLocale());
        String filename = invoice.getNumber() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER')")
    public String issue(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        return transition(id, redirectAttributes, () -> invoiceService.issue(id), "invoice.flash.issued");
    }

    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER')")
    public String markPaid(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        return transition(id, redirectAttributes, () -> invoiceService.markPaid(id), "invoice.flash.paid");
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER')")
    public String cancel(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        return transition(id, redirectAttributes, () -> invoiceService.cancel(id), "invoice.flash.cancelled");
    }

    private String transition(UUID id, RedirectAttributes redirectAttributes, Runnable action, String flashKey) {
        try {
            action.run();
            redirectAttributes.addFlashAttribute("successMessage", flashKey);
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/invoices/" + id;
    }

    private boolean canManage(Invoice invoice) {
        return invoice.getCustomer() != null
                && accessControlService.canManageInvoicesForCustomer(invoice.getCustomer());
    }

    private YearMonth resolvePeriod(Integer year, Integer month) {
        if (year != null && month != null) {
            return YearMonth.of(year, month);
        }
        return YearMonth.now().minusMonths(1);
    }
}
