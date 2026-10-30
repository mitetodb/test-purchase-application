package app.controller;

import app.service.InviteTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/invites")
@PreAuthorize("hasAnyRole('ADMIN','ACCOUNT_MANAGER')")
public class InviteController {

    private final InviteTokenService inviteTokenService;

    @GetMapping
    public String showInvitePage() {
        return "invites/invite-create";
    }

    @PostMapping("/create")
    public String createInvite(Model model, RedirectAttributes redirectAttributes) {
        try {
            String rawToken = inviteTokenService.createInviteToken();
            String inviteLink = "/register?token=" + rawToken;
            
            redirectAttributes.addFlashAttribute("inviteLink", inviteLink);
            redirectAttributes.addFlashAttribute("successMessage", "Invite created successfully!");
            
            return "redirect:/invites";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "invites/invite-create";
        }
    }
}
