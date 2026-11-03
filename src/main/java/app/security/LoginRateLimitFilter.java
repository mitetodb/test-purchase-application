package app.security;

import app.security.TwoFactorSessionKeys;
import app.service.LoginSecurityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private final LoginSecurityService loginSecurityService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (isLoginPost(request)) {
            String clientIp = ClientIpResolver.resolve(request);
            String username = request.getParameter("username");
            if (loginSecurityService.isRateLimitExceeded(clientIp, username)) {
                response.sendRedirect(request.getContextPath() + "/login?error");
                return;
            }
        }

        if (isTwoFactorPost(request)) {
            String clientIp = ClientIpResolver.resolve(request);
            String username = resolveTwoFactorUsername(request);
            if (loginSecurityService.isRateLimitExceeded(clientIp, username)) {
                response.sendRedirect(request.getContextPath() + "/login?error");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private static String resolveTwoFactorUsername(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object flag = session.getAttribute(TwoFactorSessionKeys.PASSWORD_LOGIN);
        if (!Boolean.TRUE.equals(flag)) {
            return null;
        }
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    private static boolean isTwoFactorPost(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        return uri.endsWith("/2fa/verify")
                || uri.endsWith("/2fa/enroll")
                || uri.endsWith("/2fa/recovery");
    }

    private static boolean isLoginPost(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().endsWith("/login");
    }
}
