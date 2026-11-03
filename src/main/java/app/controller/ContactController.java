package app.controller;

import app.security.ClientIpResolver;
import app.service.ContactService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping("/contact")
    public String submitContact(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String privacy,
            @RequestParam(required = false) String website,
            @RequestParam(defaultValue = "0") long ts,
            HttpServletRequest request) {

        String clientIp = ClientIpResolver.resolve(request);

        if (contactService.isHoneypotFilled(website)) {
            return "redirect:/?sent=1#kontakt";
        }

        if (contactService.isRateLimitExceeded(clientIp)) {
            return "redirect:/?sent=0#kontakt";
        }

        contactService.recordSubmission(clientIp);

        if (contactService.isTooSoon(ts)
                || !contactService.isValidSubmission(name, company, email, message, privacy)) {
            return "redirect:/?sent=0#kontakt";
        }

        boolean sent = contactService.sendContactEmail(name, company, email, message);
        return "redirect:/?sent=" + (sent ? "1" : "0") + "#kontakt";
    }
}
