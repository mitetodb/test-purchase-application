package app.controller;

import app.config.AppListExportProperties;
import app.model.dto.TestPurchaseCreateDTO;
import app.model.dto.TestPurchaseEditDTO;
import app.model.dto.TestPurchaseViewDTO;
import app.model.entity.TestPurchase;
import app.model.enums.TestPurchaseStatus;
import app.model.filter.TestPurchaseListFilter;
import app.service.*;
import app.support.ListExportResponseFactory;
import app.support.ListExportRows;
import app.support.ListFilterRedirectBuilder;
import app.support.ListPagingSupport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import com.lowagie.text.DocumentException;

@Controller
@RequiredArgsConstructor
@RequestMapping("/testpurchases")
public class TestPurchaseController {

    private final TestPurchaseService testPurchaseService;
    private final CustomerService customerService;
    private final ShopService shopService;
    private final AttachmentService attachmentService;
    private final StatusHistoryService statusHistoryService;
    private final QuestionnaireService questionnaireService;
    private final AccessControlService accessControlService;
    private final UserService userService;
    private final ListPagingSupport listPagingSupport;
    private final AppListExportProperties exportProperties;
    private final ListExportResponseFactory exportResponses;
    private final VisitAppointmentService visitAppointmentService;
    private final TestPurchaseReportService testPurchaseReportService;

    @GetMapping
    public String list(
            @ModelAttribute("filter") TestPurchaseListFilter filter,
            @PageableDefault(size = 20, sort = "createdOn") Pageable pageable,
            Model model) {
        Pageable safePageable = listPagingSupport.forTestPurchases(pageable);
        Page<TestPurchaseViewDTO> purchasePage =
                testPurchaseService.findPageForCurrentUser(filter, safePageable);
        model.addAttribute("purchasePage", purchasePage);
        model.addAttribute("purchases", purchasePage.getContent());
        model.addAttribute("showArchived", filter.isShowArchived());
        model.addAttribute("filterCustomers", customerService.findAllForCurrentUser());
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_ACCOUNT_MANAGER".equals(a.getAuthority()))) {
            model.addAttribute("mysteryShoppers", userService.findAllMysteryShoppers());
            model.addAttribute("canFilterMysteryShopper", true);
        }
        model.addAttribute("statuses", TestPurchaseStatus.values());
        model.addAttribute("listSort",
                app.support.ListSortAllowlist.toRequestSortParameter(
                        safePageable.getSort(), app.support.ListSortAllowlist.ListKind.TEST_PURCHASE));
        return "testpurchases/testpurchases-list";
    }

    @GetMapping("/export.csv")
    public Object exportCsv(
            @ModelAttribute("filter") TestPurchaseListFilter filter,
            @PageableDefault(size = 20, sort = "createdOn") Pageable pageable,
            RedirectAttributes redirectAttributes) throws IOException {
        return exportSpreadsheet(filter, pageable, redirectAttributes, false);
    }

    @GetMapping("/export.xlsx")
    public Object exportExcel(
            @ModelAttribute("filter") TestPurchaseListFilter filter,
            @PageableDefault(size = 20, sort = "createdOn") Pageable pageable,
            RedirectAttributes redirectAttributes) throws IOException {
        return exportSpreadsheet(filter, pageable, redirectAttributes, true);
    }

    private Object exportSpreadsheet(
            TestPurchaseListFilter filter,
            Pageable pageable,
            RedirectAttributes redirectAttributes,
            boolean excel) throws IOException {
        Pageable sort = listPagingSupport.sortOnlyForTestPurchases(pageable);
        long count = testPurchaseService.countForList(filter);
        String redirectPath = ListFilterRedirectBuilder.testPurchases(filter);
        if (count > exportProperties.getMaxRows()) {
            return exportResponses.redirectWhenLimited(count, redirectPath, redirectAttributes);
        }
        boolean includeUpdatedBy = isAdmin();
        var purchases = testPurchaseService.findAllForListExport(filter, sort, exportProperties.getMaxRows());
        var header = ListExportRows.testPurchasesHeader(includeUpdatedBy);
        var dataRows = purchases.stream()
                .map(p -> ListExportRows.testPurchaseRow(p, includeUpdatedBy))
                .toList();
        if (excel) {
            return exportResponses.excelResponse("test-purchases.xlsx", "Test Purchases", header, dataRows);
        }
        return exportResponses.csvResponse("test-purchases.csv", header, dataRows);
    }

    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @GetMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER','CUSTOMER')")
    public String showAddForm(Model model) {
        if (!model.containsAttribute("dto")) {
            TestPurchaseCreateDTO dto = new TestPurchaseCreateDTO();
            dto.getItems().add(new app.model.dto.ItemDTO());
            model.addAttribute("dto", dto);
        }

        model.addAttribute("customers", customerService.findAllForCurrentUser());
        model.addAttribute("shops", shopService.findAll());
        return "testpurchases/testpurchase-add";
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER','CUSTOMER')")
    public String add(
            @Valid @ModelAttribute("dto") TestPurchaseCreateDTO dto,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("customers", customerService.findAllForCurrentUser());
            model.addAttribute("shops", shopService.findAll());
            return "testpurchases/testpurchase-add";
        }

        testPurchaseService.create(dto);;
        return "redirect:/testpurchases";
    }

    @PostMapping("/add/calculate-price")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER','CUSTOMER')")
    public String calculatePriceOnCreate(
            @ModelAttribute("dto") TestPurchaseCreateDTO dto,
            Model model
    ) {
        try {
            var price = testPurchaseService.previewPrice(dto);
            model.addAttribute("price", price);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("priceError", ex.getMessage());
        }

        model.addAttribute("customers", customerService.findAllForCurrentUser());
        model.addAttribute("shops", shopService.findAll());

        return "testpurchases/testpurchase-add";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER')")
    public String showEditForm(@PathVariable UUID id, Model model) {
        var purchase = testPurchaseService.findByIdForEdit(id);

        TestPurchaseEditDTO dto = new TestPurchaseEditDTO();
        dto.setId(purchase.getId());
        dto.setCustomerId(purchase.getCustomer().getId());
        dto.setShopId(purchase.getShop().getId());
        dto.setCountry(purchase.getCountry());
        dto.setCategory(purchase.getCategory());
        dto.setType(purchase.getType());
        dto.setDeadlineAt(purchase.getDeadlineAt());

        purchase.getItems().forEach(i -> {
            app.model.dto.ItemDTO itemDTO = new app.model.dto.ItemDTO();
            itemDTO.setId(i.getId());
            itemDTO.setProductUrl(i.getProductUrl());
            itemDTO.setProductName(i.getProductName());
            itemDTO.setQuantity(i.getQuantity());
            itemDTO.setUnitPrice(i.getUnitPrice());
            dto.getItems().add(itemDTO);
        });

        model.addAttribute("dto", dto);
        model.addAttribute("customers", customerService.findAllForCurrentUser());
        model.addAttribute("shops", shopService.findAll());
        model.addAttribute("purchase", purchase);
        return "testpurchases/testpurchase-edit";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER')")
    public String edit(
            @PathVariable UUID id,
            @Valid @ModelAttribute("dto") TestPurchaseEditDTO dto,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("customers", customerService.findAllForCurrentUser());
            model.addAttribute("shops", shopService.findAll());
            return "testpurchases/testpurchase-edit";
        }
        model.addAttribute("testPurchaseDTO", dto);
        testPurchaseService.edit(id, dto);
        return "redirect:/testpurchases";
    }

    @PostMapping("/archive/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String archive(@PathVariable UUID id) {
        testPurchaseService.archive(id);
        return "redirect:/testpurchases";
    }

    @PostMapping("/unarchive/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String unarchive(@PathVariable UUID id) {
        testPurchaseService.unarchive(id);
        return "redirect:/testpurchases/view/" + id;
    }

    @GetMapping("/view/{id}/report.pdf")
    public ResponseEntity<byte[]> downloadPdfReport(@PathVariable UUID id) throws DocumentException {
        var report = testPurchaseReportService.buildPdfReport(id, LocaleContextHolder.getLocale());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(report.downloadFilename())
                        .build()
                        .toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(report.content());
    }

    @GetMapping("/view/{id}")
    public String view(@PathVariable UUID id, Model model) {
        TestPurchaseViewDTO purchase = testPurchaseService.findByIdForCurrentUser(id);
        TestPurchase entity = testPurchaseService.findEntityByIdForCurrentUser(id);
        model.addAttribute("purchase", purchase);
        model.addAttribute("attachments", attachmentService.getByTestPurchase(id));
        model.addAttribute("history", statusHistoryService.getHistory(id));
        model.addAttribute("statuses", java.util.Arrays.stream(TestPurchaseStatus.values())
                .filter(s -> !s.isArchived())
                .toList());
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_ACCOUNT_MANAGER".equals(a.getAuthority()))) {
            model.addAttribute("mysteryShoppers", userService.findAllMysteryShoppers());
        }
        model.addAttribute("canManageVisitAppointment", accessControlService.canManageVisitAppointment(entity));
        model.addAttribute("canFillQuestionnaire", accessControlService.canFillQuestionnaire(entity));
        model.addAttribute("hasQuestionnaire", questionnaireService.hasResponse(id));
        if (questionnaireService.hasResponse(id)) {
            model.addAttribute("questionnaireResult", questionnaireService.getResultForTestPurchase(id));
        }
        return "testpurchases/testpurchase-view";
    }

    private static final DateTimeFormatter VISIT_APPOINTMENT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @PostMapping("/{id}/visit-appointment")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER','MYSTERY_SHOPPER')")
    public String scheduleVisitAppointment(@PathVariable UUID id,
                                           @RequestParam String scheduledAt,
                                           @RequestParam(required = false) String location,
                                           @RequestParam(required = false) String notes,
                                           @RequestParam(required = false, defaultValue = "false") boolean confirm,
                                           RedirectAttributes redirectAttributes) {
        return handleVisitAppointment(id, scheduledAt, location, notes, confirm, redirectAttributes);
    }

    @PostMapping("/{id}/visit-appointment/confirm")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER','MYSTERY_SHOPPER')")
    public String confirmVisitAppointment(@PathVariable UUID id,
                                          RedirectAttributes redirectAttributes) {
        try {
            visitAppointmentService.confirm(id);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("appointmentError", ex.getMessage());
        }
        return "redirect:/testpurchases/view/" + id;
    }

    private String handleVisitAppointment(UUID id,
                                          String scheduledAt,
                                          String location,
                                          String notes,
                                          boolean confirm,
                                          RedirectAttributes redirectAttributes) {
        try {
            LocalDateTime at = LocalDateTime.parse(scheduledAt.trim(), VISIT_APPOINTMENT_FORMAT);
            visitAppointmentService.scheduleOrUpdate(id, at, location, notes, confirm);
        } catch (DateTimeParseException ex) {
            redirectAttributes.addFlashAttribute("appointmentError", "Invalid date and time");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("appointmentError", ex.getMessage());
        }
        return "redirect:/testpurchases/view/" + id;
    }

    @PostMapping("/{id}/change-status")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER','MYSTERY_SHOPPER')")
    public String changeStatus(@PathVariable UUID id,
                               @RequestParam TestPurchaseStatus newStatus,
                               @RequestParam(required = false) String comment) {

        testPurchaseService.changeStatus(id, newStatus, comment);

        return "redirect:/testpurchases/view/" + id;
    }

    @PostMapping("/{id}/assign-mystery-shopper")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER')")
    public String assignMysteryShopper(@PathVariable UUID id,
                                       @RequestParam(required = false) UUID mysteryShopperId) {
        testPurchaseService.assignMysteryShopperToTestPurchase(id, mysteryShopperId);
        return "redirect:/testpurchases/view/" + id;
    }

}
