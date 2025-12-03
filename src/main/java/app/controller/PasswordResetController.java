package app.controller;

import app.model.dto.ForgotPasswordDTO;
import app.model.dto.ResetPasswordDTO;
import app.model.entity.PasswordResetToken;
import app.model.entity.User;
import app.service.EmailService;
import app.service.PasswordResetService;
import app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;
    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        if (!model.containsAttribute("forgotPasswordDTO")) {
            model.addAttribute("forgotPasswordDTO", new ForgotPasswordDTO());
        }
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(
            @Valid @ModelAttribute("forgotPasswordDTO") ForgotPasswordDTO dto,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }

        userService.findByEmail(dto.getEmail()).ifPresent(user -> {
            String token = passwordResetService.createTokenForUser(user);

            String link = baseUrl + "/reset-password?token=" + token; // adapt with absolute URL if needed
            emailService.sendResetPasswordEmail(
                    user.getEmail(),
                    user.getUsername(),
                    link);
        });

        model.addAttribute("message", "If this email exists, a reset link has been sent.");
        return "auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam("token") String token, Model model) {
        ResetPasswordDTO dto = new ResetPasswordDTO();
        dto.setToken(token);
        model.addAttribute("resetPasswordDTO", dto);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(
            @Valid @ModelAttribute("resetPasswordDTO") ResetPasswordDTO dto,
            BindingResult bindingResult,
            Model model
    ) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword", "Passwords do not match.");
        }

        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }

        try {
            PasswordResetToken token = passwordResetService.validateToken(dto.getToken());
            User user = token.getUser();

            userService.changePassword(user, dto.getPassword());
            passwordResetService.markUsed(token);

            return "redirect:/login?passwordReset";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/reset-password";
        }
    }
}
