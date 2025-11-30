package app.controller;

import app.model.dto.UserProfileDTO;
import app.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping("/profile")
    public String showProfile(Authentication authentication, Model model) {
        String username = authentication.getName();

        if (!model.containsAttribute("profile")) {
            UserProfileDTO dto = userService.getProfile(username);
            model.addAttribute("profile", dto);
        }

        model.addAttribute("countries", app.model.enums.Country.values());
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(
            Authentication authentication,
            @Valid @ModelAttribute("profile") UserProfileDTO profile,
            BindingResult bindingResult,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        String username = authentication.getName();

        if (bindingResult.hasErrors()) {
            model.addAttribute("countries", app.model.enums.Country.values());
            return "profile";
        }

        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                imageUrl = saveAvatar(imageFile, username);
            } catch (IOException e) {
                bindingResult.reject("imageFile", "Cannot store file.");
                model.addAttribute("countries", app.model.enums.Country.values());
                return "profile";
            }
        }

        userService.updateProfile(username, profile, imageUrl);

        redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
        return "redirect:/profile";
    }

    private String saveAvatar(MultipartFile file, String username) throws IOException {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String ext = "";
        int dot = originalFilename.lastIndexOf('.');
        if (dot >= 0) {
            ext = originalFilename.substring(dot);
        }

        String newFileName = username + "-" + System.currentTimeMillis() + ext;
        Path uploadDir = Paths.get("uploads/avatars");
        Files.createDirectories(uploadDir);

        Path target = uploadDir.resolve(newFileName);
        Files.copy(file.getInputStream(), target);

        // this URL will be handled by WebConfig
        return "/avatars/" + newFileName;
    }
}

