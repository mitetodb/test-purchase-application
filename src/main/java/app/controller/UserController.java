package app.controller;

import app.model.dto.UserCreateDTO;
import app.model.dto.UserEditDTO;
import app.model.entity.User;
import app.model.enums.Country;
import app.model.enums.Role;
import app.repository.UserRepository;
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
    public String editForm(@PathVariable UUID id, Model model) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserEditDTO userEditDTO = new UserEditDTO();
        userEditDTO.setId(user.getId());
        userEditDTO.setUsername(user.getUsername());
        userEditDTO.setEmail(user.getEmail());
        userEditDTO.setRole(user.getRole());
        userEditDTO.setCountry(user.getCountry());
        userEditDTO.setActive(user.isActive());

        model.addAttribute("userEditDTO", userEditDTO);
        model.addAttribute("roles", Role.values());
        model.addAttribute("countries", Country.values());
        return "users/users-edit";
    }

    @PostMapping("/edit/{id}")
    public String editUser(
            @PathVariable UUID id,
            @Valid @ModelAttribute("userEditDTO") UserEditDTO userEditDTO,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("roles", Role.values());
            model.addAttribute("countries", Country.values());
            return "users/users-edit";
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setUsername(userEditDTO.getUsername());
        user.setEmail(userEditDTO.getEmail());
        user.setCountry(userEditDTO.getCountry());
        user.setRole(userEditDTO.getRole());
        user.setActive(userEditDTO.isActive());

        userRepository.save(user);

        return "redirect:/users";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable UUID id) {
        userRepository.deleteById(id);
        return "redirect:/users";
    }
}