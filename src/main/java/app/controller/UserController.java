package app.controller;

import app.model.dto.UserCreateDTO;
import app.model.dto.UserEditDTO;
import app.model.entity.Customer;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.UserRepository;
import app.service.CustomerService;
import app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final CustomerService customerService;

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "users/users-list";
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

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable UUID id) {
        userRepository.deleteById(id);
        return "redirect:/users";
    }
}