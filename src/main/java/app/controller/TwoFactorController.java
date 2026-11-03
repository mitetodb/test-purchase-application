package app.controller;

import app.config.TwoFactorSecurityProperties;
import app.model.entity.User;
import app.repository.UserRepository;
import app.security.TwoFactorSessionKeys;
import app.security.TwoFactorSessionSupport;
import app.service.LoginSecurityService;
import app.service.TwoFactorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private final UserRepository userRepository;
    private final TwoFactorService twoFactorService;
    private final LoginSecurityService loginSecurityService;
    private final TwoFactorSessionSupport twoFactorSessionSupport;
    private final TwoFactorSecurityProperties twoFactorSecurityProperties;

    @GetMapping("/enroll")
    public String enrollForm(
            Authentication authentication,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        ResolvedUser resolved = resolvePrivilegedUser(authentication, session, request, response);
        if (resolved.redirect() != null) {
            return resolved.redirect();
        }
        User user = resolved.user();
        if (user.isTotpEnabled()) {
            return "redirect:/2fa/verify";
        }
        String secret = (String) session.getAttribute(TwoFactorSessionKeys.ENROLLMENT_SECRET);
        if (secret == null) {
            secret = twoFactorService.generateSecret();
            session.setAttribute(TwoFactorSessionKeys.ENROLLMENT_SECRET, secret);
        }
        model.addAttribute("otpAuthUri", twoFactorService.buildOtpAuthUri(user.getUsername(), secret));
        model.addAttribute("qrCodeDataUri", twoFactorService.buildQrCodeDataUri(user.getUsername(), secret));
        model.addAttribute("manualSecret", secret);
        return "auth/totp-enroll";
    }

    @PostMapping("/enroll")
    public String confirmEnrollment(
            Authentication authentication,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam("code") String code,
            RedirectAttributes redirectAttributes) {

        ResolvedUser resolved = resolvePrivilegedUser(authentication, session, request, response);
        if (resolved.redirect() != null) {
            return resolved.redirect();
        }
        User user = resolved.user();
        String secret = (String) session.getAttribute(TwoFactorSessionKeys.ENROLLMENT_SECRET);
        if (secret == null) {
            return "redirect:/2fa/enroll";
        }

        User fresh = userRepository.findByUsername(user.getUsername()).orElseThrow();
        String lockRedirect = redirectIfAccountLocked(fresh, session, request, response);
        if (lockRedirect != null) {
            return lockRedirect;
        }

        List<String> recoveryCodes;
        try {
            recoveryCodes = twoFactorService.completeEnrollmentAndIssueRecoveryCodes(fresh, secret, code);
        } catch (IllegalArgumentException ex) {
            String failureRedirect = handleTwoFactorFailure(session, request, response, fresh.getUsername());
            if (failureRedirect != null) {
                return failureRedirect;
            }
            redirectAttributes.addFlashAttribute("errorMessageKey", ex.getMessage());
            return "redirect:/2fa/enroll";
        }

        session.removeAttribute(TwoFactorSessionKeys.ENROLLMENT_SECRET);
        session.removeAttribute(TwoFactorSessionKeys.FAILED_ATTEMPTS);
        session.setAttribute(TwoFactorSessionKeys.VERIFIED, Boolean.TRUE);
        redirectAttributes.addFlashAttribute("recoveryCodes", recoveryCodes);
        return "redirect:/2fa/recovery-codes";
    }

    @GetMapping("/recovery-codes")
    public String showRecoveryCodes(
            Authentication authentication,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        ResolvedUser resolved = resolvePrivilegedUser(authentication, session, request, response);
        if (resolved.redirect() != null) {
            return resolved.redirect();
        }
        if (!Boolean.TRUE.equals(session.getAttribute(TwoFactorSessionKeys.VERIFIED))) {
            return "redirect:/2fa/enroll";
        }
        return "auth/totp-recovery-codes";
    }

    @GetMapping("/verify")
    public String verifyForm(
            Authentication authentication,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {
        ResolvedUser resolved = resolvePrivilegedUser(authentication, session, request, response);
        if (resolved.redirect() != null) {
            return resolved.redirect();
        }
        User user = resolved.user();
        if (!user.isTotpEnabled()) {
            return "redirect:/2fa/enroll";
        }
        return "auth/totp-verify";
    }

    @PostMapping("/verify")
    public String verifyCode(
            Authentication authentication,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam("code") String code,
            RedirectAttributes redirectAttributes) {

        ResolvedUser resolved = resolvePrivilegedUser(authentication, session, request, response);
        if (resolved.redirect() != null) {
            return resolved.redirect();
        }
        User user = resolved.user();
        if (!user.isTotpEnabled()) {
            return "redirect:/2fa/enroll";
        }

        User fresh = userRepository.findByUsername(user.getUsername()).orElseThrow();
        String lockRedirect = redirectIfAccountLocked(fresh, session, request, response);
        if (lockRedirect != null) {
            return lockRedirect;
        }

        if (twoFactorService.verifyTotpCode(fresh, code)) {
            session.removeAttribute(TwoFactorSessionKeys.FAILED_ATTEMPTS);
            session.setAttribute(TwoFactorSessionKeys.VERIFIED, Boolean.TRUE);
            return "redirect:/home";
        }

        String failureRedirect = handleTwoFactorFailure(session, request, response, fresh.getUsername());
        if (failureRedirect != null) {
            return failureRedirect;
        }
        redirectAttributes.addFlashAttribute("errorMessageKey", "twoFactor.error.invalidCode");
        return "redirect:/2fa/verify";
    }

    @PostMapping("/recovery")
    public String verifyRecoveryCode(
            Authentication authentication,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam("recoveryCode") String recoveryCode,
            RedirectAttributes redirectAttributes) {

        ResolvedUser resolved = resolvePrivilegedUser(authentication, session, request, response);
        if (resolved.redirect() != null) {
            return resolved.redirect();
        }
        User user = resolved.user();
        User fresh = userRepository.findByUsername(user.getUsername()).orElseThrow();
        String lockRedirect = redirectIfAccountLocked(fresh, session, request, response);
        if (lockRedirect != null) {
            return lockRedirect;
        }

        if (twoFactorService.consumeRecoveryCode(fresh, recoveryCode)) {
            session.removeAttribute(TwoFactorSessionKeys.FAILED_ATTEMPTS);
            session.setAttribute(TwoFactorSessionKeys.VERIFIED, Boolean.TRUE);
            redirectAttributes.addFlashAttribute("successMessageKey", "twoFactor.recovery.used");
            return "redirect:/home";
        }

        String failureRedirect = handleTwoFactorFailure(session, request, response, fresh.getUsername());
        if (failureRedirect != null) {
            return failureRedirect;
        }
        redirectAttributes.addFlashAttribute("errorMessageKey", "twoFactor.error.invalidRecovery");
        return "redirect:/2fa/verify";
    }

    private record ResolvedUser(User user, String redirect) {}

    private ResolvedUser resolvePrivilegedUser(
            Authentication authentication,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response) {
        if (session == null || !Boolean.TRUE.equals(session.getAttribute(TwoFactorSessionKeys.PASSWORD_LOGIN))) {
            return new ResolvedUser(null, "redirect:/login");
        }
        if (authentication == null || authentication.getName() == null) {
            return new ResolvedUser(null, "redirect:/login");
        }
        User user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null || !twoFactorService.requiresTwoFactor(user)) {
            return new ResolvedUser(null, "redirect:/login");
        }
        return new ResolvedUser(user, null);
    }

    private String redirectIfAccountLocked(
            User user, HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        if (loginSecurityService.isCurrentlyLocked(user)) {
            twoFactorSessionSupport.invalidatePendingTwoFactorSession(request, response, session);
            return "redirect:/login?error";
        }
        return null;
    }

    private String handleTwoFactorFailure(
            HttpSession session, HttpServletRequest request, HttpServletResponse response, String username) {
        loginSecurityService.handleFailedLogin(app.security.ClientIpResolver.resolve(request), username);
        int failures = twoFactorSessionSupport.incrementFailedAttempts(session);
        if (failures >= twoFactorSecurityProperties.maxSessionFailedAttempts()) {
            twoFactorSessionSupport.invalidatePendingTwoFactorSession(request, response, session);
            return "redirect:/login?error";
        }
        return null;
    }
}
