package app.controller;

import app.config.AppListExportProperties;
import app.model.dto.CustomerDTO;
import app.model.entity.Customer;
import app.model.filter.CustomerListFilter;
import app.service.CustomerService;
import app.model.enums.Country;
import app.support.ListExportResponseFactory;
import app.support.ListExportRows;
import app.support.ListFilterRedirectBuilder;
import app.support.ListPagingSupport;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/customers")
@PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER','SALES_MANAGER')")
public class CustomerController {

    private final CustomerService customerService;
    private final ListPagingSupport listPagingSupport;
    private final AppListExportProperties exportProperties;
    private final ListExportResponseFactory exportResponses;

    @GetMapping
    public String listCustomers(
            @ModelAttribute("filter") CustomerListFilter filter,
            @PageableDefault(size = 20, sort = "name") Pageable pageable,
            Model model) {
        Pageable safePageable = listPagingSupport.forCustomers(pageable);
        Page<Customer> customerPage = customerService.findPageForCurrentUser(filter, safePageable);
        model.addAttribute("customerPage", customerPage);
        model.addAttribute("customers", customerPage.getContent());
        model.addAttribute("countries", Country.values());
        model.addAttribute("listSort",
                app.support.ListSortAllowlist.toRequestSortParameter(
                        safePageable.getSort(), app.support.ListSortAllowlist.ListKind.CUSTOMER));
        return "customers/customers-list";
    }

    @GetMapping("/export.csv")
    public Object exportCsv(
            @ModelAttribute("filter") CustomerListFilter filter,
            @PageableDefault(size = 20, sort = "name") Pageable pageable,
            RedirectAttributes redirectAttributes) throws IOException {
        return exportSpreadsheet(filter, pageable, redirectAttributes, false);
    }

    @GetMapping("/export.xlsx")
    public Object exportExcel(
            @ModelAttribute("filter") CustomerListFilter filter,
            @PageableDefault(size = 20, sort = "name") Pageable pageable,
            RedirectAttributes redirectAttributes) throws IOException {
        return exportSpreadsheet(filter, pageable, redirectAttributes, true);
    }

    private Object exportSpreadsheet(
            CustomerListFilter filter,
            Pageable pageable,
            RedirectAttributes redirectAttributes,
            boolean excel) throws IOException {
        Pageable sort = listPagingSupport.sortOnlyForCustomers(pageable);
        long count = customerService.countForList(filter);
        String redirectPath = ListFilterRedirectBuilder.customers(filter);
        if (count > exportProperties.getMaxRows()) {
            return exportResponses.redirectWhenLimited(count, redirectPath, redirectAttributes);
        }
        boolean includeUpdatedBy = isAdmin();
        var customers = customerService.findAllForListExport(filter, sort, exportProperties.getMaxRows());
        var header = ListExportRows.customersHeader(includeUpdatedBy);
        var dataRows = customers.stream().map(c -> ListExportRows.customerRow(c, includeUpdatedBy)).toList();
        if (excel) {
            return exportResponses.excelResponse("customers.xlsx", "Customers", header, dataRows);
        }
        return exportResponses.csvResponse("customers.csv", header, dataRows);
    }

    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("customerDTO", new CustomerDTO());
        return "customers/customers-add";
    }

    @PostMapping("/add")
    public String addCustomer(@Valid @ModelAttribute CustomerDTO customerDTO,
                              BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "customers/customers-add";
        }

        customerService.create(customerDTO);
        return "redirect:/customers";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable UUID id, Model model) {
        Customer customer = customerService.findByIdForCurrentUser(id);

        CustomerDTO dto = new CustomerDTO();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setCategory(customer.getCategory());
        dto.setCountry(customer.getCountry());
        dto.setEmail(customer.getEmail());
        dto.setBaseServiceFee(customer.getBaseServiceFee());

        model.addAttribute("customerDTO", dto);

        return "customers/customers-edit";
    }

    @PostMapping("/edit/{id}")
    public String editCustomer(@PathVariable UUID id,
                               @Valid @ModelAttribute CustomerDTO dto,
                               BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "customers/customers-edit";
        }

        customerService.update(id, dto);
        return "redirect:/customers";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable UUID id) {
        customerService.delete(id);
        return "redirect:/customers";
    }
}