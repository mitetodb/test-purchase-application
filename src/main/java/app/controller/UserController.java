package app.controller;

import app.config.AppListExportProperties;
import app.model.dto.UserCreateDTO;
import app.model.dto.UserEditDTO;
import app.model.entity.Customer;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.model.filter.UserListFilter;
import app.repository.UserRepository;
import app.service.CustomerService;
import app.service.UserService;
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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final CustomerService customerService;
    private final ListPagingSupport listPagingSupport;
    private final AppListExportProperties exportProperties;
    private final ListExportResponseFactory exportResponses;

    @GetMapping
    public String listUsers(
            @ModelAttribute("filter") UserListFilter filter,
            @PageableDefault(size = 20, sort = "createdOn") Pageable pageable,
            Model model) {
        Pageable safePageable = listPagingSupport.forUsers(pageable);
        Page<User> userPage = userService.findPageForAdmin(filter, safePageable);
        model.addAttribute("userPage", userPage);
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("roles", Role.values());
        model.addAttribute("countries", Country.values());
        return "users/users-list";
    }

    @GetMapping("/export.csv")
    public Object exportCsv(
            @ModelAttribute("filter") UserListFilter filter,
            @PageableDefault(size = 20, sort = "createdOn") Pageable pageable,
            RedirectAttributes redirectAttributes) throws IOException {
        return exportSpreadsheet(filter, pageable, redirectAttributes, false);
    }

    @GetMapping("/export.xlsx")
    public Object exportExcel(
            @ModelAttribute("filter") UserListFilter filter,
            @PageableDefault(size = 20, sort = "createdOn") Pageable pageable,
            RedirectAttributes redirectAttributes) throws IOException {
        return exportSpreadsheet(filter, pageable, redirectAttributes, true);
    }

    private Object exportSpreadsheet(
            UserListFilter filter,
            Pageable pageable,
            RedirectAttributes redirectAttributes,
            boolean excel) throws IOException {
        Pageable sort = listPagingSupport.sortOnlyForUsers(pageable);
        long count = userService.countForList(filter);
        String redirectPath = ListFilterRedirectBuilder.users(filter);
        if (count > exportProperties.getMaxRows()) {
            return exportResponses.redirectWhenLimited(count, redirectPath, redirectAttributes);
        }
        var users = userService.findAllForListExport(filter, sort, exportProperties.getMaxRows());
        var header = ListExportRows.usersHeader(true);
        var dataRows = users.stream().map(u -> ListExportRows.userRow(u, true)).toList();
        if (excel) {
            return exportResponses.excelResponse("users.xlsx", "Users", header, dataRows);
        }
        return exportResponses.csvResponse("users.csv", header, dataRows);
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("userCreateDTO", new UserCreateDTO());
        model.addAttribute("roles", Role.values());
        model.addAttribute("countries", Country.values());
        return "users/users-add";
    }

    @PostMapping("/add")
    public String addUser(
            @Valid @ModelAttribute("userCreateDTO") UserCreateDTO userCreateDTO,
            BindingResult bindingResult,
            Model model) {

        if (!userCreateDTO.getPassword().equals(userCreateDTO.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Passwords must match");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", Role.values());
            model.addAttribute("countries", Country.values());
            return "users/users-add";
        }

        userService.createUser(userCreateDTO);
        return "redirect:/users";
    }

    @GetMapping("/edit/{id}")
    public String showEditUserForm(@PathVariable UUID id, Model model) {
        User user = userService.findByIdWithManagedCustomers(id);

        UserEditDTO dto = new UserEditDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setCountry(user.getCountry());
        dto.setRole(user.getRole());
        dto.setActive(user.isActive());

        if (user.getManagedCustomers() != null) {
            dto.setManagedCustomerIds(
                    user.getManagedCustomers()
                            .stream()
                            .map(Customer::getId)
                            .toList()
            );
        }

        model.addAttribute("userEditDTO", dto);
        model.addAttribute("allRoles", Role.values());
        model.addAttribute("allCustomers", customerService.findAll());

        return "users/users-edit";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String editUser(
            @PathVariable UUID id,
            @Valid @ModelAttribute("userEditDTO") UserEditDTO dto,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allRoles", Role.values());
            model.addAttribute("allCustomers", customerService.findAll());
            return "users/users-edit";
        }

        userService.updateUserFromDto(id, dto);

        return "redirect:/users";
    }

    @PostMapping("/deactivate/{id}")
    public String deactivate(@PathVariable UUID id) {
        userService.deactivateUser(id);
        return "redirect:/users";
    }

    @PostMapping("/reactivate/{id}")
    public String reactivate(@PathVariable UUID id) {
        userService.reactivateUser(id);
        return "redirect:/users";
    }

    @PostMapping("/unlock/{id}")
    public String unlock(@PathVariable UUID id) {
        userService.unlockAccount(id);
        return "redirect:/users";
    }
}