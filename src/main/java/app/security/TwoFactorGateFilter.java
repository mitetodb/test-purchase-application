package app.security;

import app.model.entity.User;
import app.model.enums.Role;
import app.repository.UserRepository;
import app.service.TwoFactorService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * After password login, Admin and Account Manager users must complete TOTP enrollment or verification
 * before accessing the rest of the app. Skipped for {@code @WithMockUser} tests (no password-login flag).
 */
@Component
@RequiredArgsConstructor
public class TwoFactorGateFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PATH_PREFIXES = Set.of(
            "/2fa/",
            "/logout",
            "/css/",
            "/js/",
            "/images/",
            "/favicon.ico",
            "/error",
            "/access-denied");

    private final UserRepository userRepository;
    private final TwoFactorService twoFactorService;
    private final TransactionTemplate transactionTemplate;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        if (session == null || !Boolean.TRUE.equals(session.getAttribute(TwoFactorSessionKeys.PASSWORD_LOGIN))) {
            filterChain.doFilter(request, response);
            return;
        }
        if (Boolean.TRUE.equals(session.getAttribute(TwoFactorSessionKeys.VERIFIED))) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        User user = transactionTemplate.execute(
                status -> userRepository.findByUsername(authentication.getName()).orElse(null));
        if (user == null || !twoFactorService.requiresTwoFactor(user)) {
            session.setAttribute(TwoFactorSessionKeys.VERIFIED, Boolean.TRUE);
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.isEmpty()) {
            path = "/";
        }

        if (isAllowedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String redirect = user.isTotpEnabled() ? "/2fa/verify" : "/2fa/enroll";
        response.sendRedirect(request.getContextPath() + redirect);
    }

    private static boolean isAllowedPath(String path) {
        for (String prefix : ALLOWED_PATH_PREFIXES) {
            if (path.equals(prefix.replaceAll("/$", "")) || path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
